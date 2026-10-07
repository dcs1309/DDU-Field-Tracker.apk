package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DataConfidence
import com.example.data.model.EvidenceEntity
import com.example.data.model.OpportunityEntity
import com.example.data.model.ProductAliasEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RecordStatus
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SurveyEntity
import com.example.data.model.SurveyType
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserRole
import com.example.data.model.UserProfile
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.data.repository.FieldIntelligenceRepository
import com.example.data.sync.FirestoreSyncManager
import com.example.data.sync.NetworkConnectivityMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UiStatistics(
    val todaysSurveysCount: Int = 4,
    val pendingSyncCount: Int = 7,
    val submittedCount: Int = 23,
    val approvedCount: Int = 18,
    val opportunitiesCount: Int = 5,
    val requiresCorrectionCount: Int = 1,
    val completenessAverage: Int = 89,
    val confidenceAverage: Int = 92
)

class FieldIntelligenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FieldIntelligenceRepository
    
    // User Profile & Authentication State with SharedPreferences
    private val prefs = application.getSharedPreferences("ddu_user_prefs", android.content.Context.MODE_PRIVATE)

    private val initialRole = try {
        UserRole.valueOf(prefs.getString("user_role", UserRole.FIELD_SURVEYOR.name) ?: UserRole.FIELD_SURVEYOR.name)
    } catch (e: Exception) {
        UserRole.FIELD_SURVEYOR
    }

    private val _currentUserProfile = MutableStateFlow(
        UserProfile(
            userId = prefs.getString("user_id", "SURV-108") ?: "SURV-108",
            name = prefs.getString("user_name", "Rajesh Kumar") ?: "Rajesh Kumar",
            email = prefs.getString("user_email", "rajesh.kumar@dri-ddu.org") ?: "rajesh.kumar@dri-ddu.org",
            phone = prefs.getString("user_phone", "+91 98765 43210") ?: "+91 98765 43210",
            role = initialRole,
            block = prefs.getString("user_block", "Balrampur") ?: "Balrampur",
            district = prefs.getString("user_district", "Chitrakoot") ?: "Chitrakoot",
            vatika = prefs.getString("user_vatika", "Rampur Tola Gram Vatika") ?: "Rampur Tola Gram Vatika",
            designation = prefs.getString("user_designation", "Senior Gram Shilpi") ?: "Senior Gram Shilpi",
            isLoggedIn = prefs.getBoolean("is_logged_in", true)
        )
    )
    val currentUserProfile: StateFlow<UserProfile> = _currentUserProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", true))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // User Role State (synced with UserProfile)
    private val _currentUserRole = MutableStateFlow(initialRole)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    // Network Connectivity & Offline Cache Simulation Monitor
    val connectivityMonitor = NetworkConnectivityMonitor(application)
    val isSimulatedOffline: StateFlow<Boolean> = connectivityMonitor.isSimulatedOffline

    // Effective online state (hardware network available and not in simulated offline)
    private val _isOnline = MutableStateFlow(connectivityMonitor.isConnected())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Last cloud sync timestamp
    private val _lastSyncTime = MutableStateFlow("Pending initial sync")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    // Night Mode (Dark Theme) State
    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    fun toggleNightMode() {
        _isNightMode.value = !_isNightMode.value
    }

    fun setNightMode(enabled: Boolean) {
        _isNightMode.value = enabled
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("Room Database active (Offline Cache)")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // Global Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Status Filter for Records
    private val _selectedStatusFilter = MutableStateFlow<String?>("ALL")
    val selectedStatusFilter: StateFlow<String?> = _selectedStatusFilter.asStateFlow()

    // Village Filter
    private val _selectedVillageFilter = MutableStateFlow<String?>("ALL")
    val selectedVillageFilter: StateFlow<String?> = _selectedVillageFilter.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FieldIntelligenceRepository(database)
        viewModelScope.launch {
            repository.initializeSeedDataIfNeeded()
        }
        // Monitor network state reactively: auto-sync pending Room cache records when coming online
        viewModelScope.launch {
            connectivityMonitor.isOnlineFlow.collect { online ->
                val wasOffline = !_isOnline.value
                _isOnline.value = online
                if (online) {
                    _syncMessage.value = "Online • Connected to Firestore"
                    if (wasOffline) {
                        // Automatically push cached offline records
                        triggerSync(isAuto = true)
                    }
                } else {
                    _syncMessage.value = "Offline Mode • Stored safely in Room Cache"
                }
            }
        }
    }

    val allSurveysWithDetails: StateFlow<List<SurveyWithDetails>> = repository.allSurveysWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncSurveysWithDetails: StateFlow<List<SurveyWithDetails>> = repository.pendingSyncSurveysWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvidence: StateFlow<List<EvidenceEntity>> = repository.allEvidence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOpportunities: StateFlow<List<OpportunityEntity>> = repository.allOpportunities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAliases: StateFlow<List<ProductAliasEntity>> = repository.allAliases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSakhyaScreenings: StateFlow<List<SakhyaScreeningEntity>> = repository.allSakhyaScreenings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStage2Assessments: StateFlow<List<VillageProductionAssessmentEntity>> = repository.allStage2Assessments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortlistedOpportunities: StateFlow<List<OpportunityEntity>> = repository.shortlistedOpportunities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stage 2 is unlocked once at least one Stage 1 product is shortlisted or assessed
    val isStage2Unlocked: StateFlow<Boolean> = combine(
        shortlistedOpportunities,
        allStage2Assessments
    ) { shortlisted, assessments ->
        shortlisted.isNotEmpty() || assessments.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Filtered Records
    val filteredSurveys: StateFlow<List<SurveyWithDetails>> = combine(
        allSurveysWithDetails,
        _searchQuery,
        _selectedStatusFilter,
        _selectedVillageFilter
    ) { surveys, query, status, village ->
        surveys.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.survey.entityName.contains(query, ignoreCase = true) ||
                item.survey.village.contains(query, ignoreCase = true) ||
                item.survey.dduId.contains(query, ignoreCase = true) ||
                item.survey.surveyType.contains(query, ignoreCase = true) ||
                item.products.any { it.productName.contains(query, ignoreCase = true) }

            val matchesStatus = when (status) {
                "ALL", null -> true
                "PENDING_SYNC" -> !item.survey.isSynced
                else -> item.survey.status == status
            }
            val matchesVillage = village == "ALL" || village == null || item.survey.village == village

            matchesQuery && matchesStatus && matchesVillage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics Calculation
    val statistics: StateFlow<UiStatistics> = allSurveysWithDetails.mapToStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiStatistics())

    private fun kotlinx.coroutines.flow.Flow<List<SurveyWithDetails>>.mapToStats() = combine(
        this,
        allOpportunities
    ) { surveys, opps ->
        val pendingSync = surveys.count { !it.survey.isSynced }
        val approved = surveys.count { it.survey.status == RecordStatus.APPROVED.name }
        val submitted = surveys.count { it.survey.status == RecordStatus.SUBMITTED.name || it.survey.status == RecordStatus.APPROVED.name }
        val returned = surveys.count { it.survey.status == RecordStatus.RETURNED.name }
        val avgCompleteness = if (surveys.isNotEmpty()) surveys.map { it.survey.completenessScore }.average().toInt() else 85
        
        UiStatistics(
            todaysSurveysCount = surveys.count { it.survey.dateString.contains("13 Sep") },
            pendingSyncCount = pendingSync,
            submittedCount = submitted,
            approvedCount = approved,
            opportunitiesCount = opps.size,
            requiresCorrectionCount = returned,
            completenessAverage = avgCompleteness,
            confidenceAverage = 92
        )
    }

    fun setUserRole(role: UserRole) {
        _currentUserRole.value = role
        val updated = _currentUserProfile.value.copy(role = role)
        _currentUserProfile.value = updated
        prefs.edit().putString("user_role", role.name).apply()
    }

    fun updateUserName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            val updated = _currentUserProfile.value.copy(name = trimmed)
            _currentUserProfile.value = updated
            prefs.edit().putString("user_name", trimmed).apply()
        }
    }

    fun updateUserProfile(
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        block: String,
        district: String,
        vatika: String,
        designation: String
    ) {
        val updated = _currentUserProfile.value.copy(
            name = name.trim().ifBlank { _currentUserProfile.value.name },
            email = email.trim().ifBlank { _currentUserProfile.value.email },
            phone = phone.trim().ifBlank { _currentUserProfile.value.phone },
            role = role,
            block = block.trim().ifBlank { _currentUserProfile.value.block },
            district = district.trim().ifBlank { _currentUserProfile.value.district },
            vatika = vatika.trim().ifBlank { _currentUserProfile.value.vatika },
            designation = designation.trim().ifBlank { _currentUserProfile.value.designation }
        )
        _currentUserProfile.value = updated
        _currentUserRole.value = role
        prefs.edit()
            .putString("user_name", updated.name)
            .putString("user_email", updated.email)
            .putString("user_phone", updated.phone)
            .putString("user_role", role.name)
            .putString("user_block", updated.block)
            .putString("user_district", updated.district)
            .putString("user_vatika", updated.vatika)
            .putString("user_designation", updated.designation)
            .apply()
    }

    fun login(
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        block: String,
        district: String
    ) {
        val updated = _currentUserProfile.value.copy(
            name = name.trim().ifBlank { "Rajesh Kumar" },
            email = email.trim().ifBlank { "surveyor@dri-ddu.org" },
            phone = phone.trim().ifBlank { "+91 98765 43210" },
            role = role,
            block = block.trim().ifBlank { "Balrampur" },
            district = district.trim().ifBlank { "Chitrakoot" },
            isLoggedIn = true
        )
        _currentUserProfile.value = updated
        _currentUserRole.value = role
        _isLoggedIn.value = true
        prefs.edit()
            .putString("user_name", updated.name)
            .putString("user_email", updated.email)
            .putString("user_phone", updated.phone)
            .putString("user_role", role.name)
            .putString("user_block", updated.block)
            .putString("user_district", updated.district)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun loginWithPreset(preset: UserProfile) {
        val updated = preset.copy(isLoggedIn = true)
        _currentUserProfile.value = updated
        _currentUserRole.value = preset.role
        _isLoggedIn.value = true
        prefs.edit()
            .putString("user_id", updated.userId)
            .putString("user_name", updated.name)
            .putString("user_email", updated.email)
            .putString("user_phone", updated.phone)
            .putString("user_role", updated.role.name)
            .putString("user_block", updated.block)
            .putString("user_district", updated.district)
            .putString("user_vatika", updated.vatika)
            .putString("user_designation", updated.designation)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun signOut() {
        _isLoggedIn.value = false
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String?) {
        _selectedStatusFilter.value = status
    }

    fun setVillageFilter(village: String?) {
        _selectedVillageFilter.value = village
    }

    fun toggleOnlineStatus() {
        connectivityMonitor.toggleSimulatedOffline()
        val isNowOnline = connectivityMonitor.isConnected()
        _isOnline.value = isNowOnline
        if (isNowOnline) {
            _syncMessage.value = "Online mode active. Syncing Room Cache..."
            triggerSync(isAuto = false)
        } else {
            _syncMessage.value = "Offline mode active: Observations cached locally in Room"
        }
    }

    fun setSimulatedOffline(offline: Boolean) {
        connectivityMonitor.setSimulatedOffline(offline)
        val isNowOnline = connectivityMonitor.isConnected()
        _isOnline.value = isNowOnline
        if (isNowOnline) {
            triggerSync(isAuto = false)
        } else {
            _syncMessage.value = "Offline mode active: Observations cached locally in Room"
        }
    }

    fun triggerSync(isAuto: Boolean = false) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = if (isAuto) "Auto-syncing Room Cache to Firestore..." else "Syncing observations with Cloud Firestore..."
            val result = repository.syncAllWithFirestore()
            _isSyncing.value = false
            _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
            _syncMessage.value = result.message
        }
    }

    fun syncWithFirestore(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Bidirectional Sync: Room Database <-> Firestore..."
            val result = repository.syncAllWithFirestore()
            _isSyncing.value = false
            _lastSyncTime.value = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
            _syncMessage.value = result.message
            onResult(result.success, result.message)
        }
    }

    // Shortlisting for Stage 2
    fun toggleShortlistOpportunity(oppId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateShortlistStatus(oppId, !currentStatus)
        }
    }

    fun shortlistOpportunity(oppId: String) {
        viewModelScope.launch {
            repository.updateShortlistStatus(oppId, true)
        }
    }

    // Stage 2 Village Production Assessment CRUD & Logic
    fun getStage2AssessmentById(id: String) = repository.getStage2AssessmentById(id)

    fun getStage2AssessmentByOppId(oppId: String) = repository.getStage2AssessmentByOppId(oppId)

    fun saveStage2Assessment(assessment: VillageProductionAssessmentEntity) {
        viewModelScope.launch {
            repository.saveStage2Assessment(assessment)
        }
    }

    fun deleteStage2Assessment(assessment: VillageProductionAssessmentEntity) {
        viewModelScope.launch {
            repository.deleteStage2Assessment(assessment)
        }
    }

    fun deleteStage2AssessmentById(id: String) {
        viewModelScope.launch {
            repository.deleteStage2AssessmentById(id)
        }
    }

    /**
     * Compute Stage 2 Decision based on Page 2 and 3 of the SWSM specification:
     * 1. Order products by Priority Ranking (1 = highest).
     * 2. Drop any product where Criticality Rating is Fail — it goes no further.
     * 3. For remaining products, check Sampling Checked. If Yes, mark Final Selection as Selected directly — Readiness Ranking is not needed.
     * 4. If Sampling Checked is No, complete Readiness Ranking (out of 7) and use it to decide Final Selection.
     */
    fun calculateStage2Decision(
        criticalityPass: Boolean,
        sampleAvailable: Boolean,
        readinessScore: Int
    ): Pair<String, Int> {
        if (!criticalityPass) {
            return Pair("Dropped (Criticality Fail)", 99)
        }
        if (sampleAvailable) {
            return Pair("Selected for Stage 3 (Direct Sample)", 1)
        }
        return when {
            readinessScore >= 6 -> Pair("Selected for Stage 3 (High Readiness)", 2)
            readinessScore >= 4 -> Pair("Suitable with Support", 3)
            else -> Pair("Developing / Low Readiness", 4)
        }
    }

    fun updateSurveyValidation(
        dduId: String,
        status: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val validator = if (_currentUserRole.value == UserRole.SUPERVISOR_VALIDATOR) {
                "Supervisor R. K. Baruah"
            } else {
                "Admin Planning Cell"
            }
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
            repository.updateValidation(dduId, status, remarks, validator, dateStr)
        }
    }

    fun updateOpportunityStage(oppId: String, newStage: String) {
        viewModelScope.launch {
            repository.updateOpportunityStage(oppId, newStage)
        }
    }

    fun updateSurveyRecord(
        survey: SurveyEntity,
        products: List<ProductEntity>? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateSurveyRecord(survey, products, _isOnline.value)
            onSuccess()
        }
    }

    fun deleteSurveyRecord(dduId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSurveyRecord(dduId)
            onSuccess()
        }
    }

    fun addProductAlias(standard: String, alias: String) {
        viewModelScope.launch {
            repository.addProductAlias(standard, alias)
        }
    }

    fun addEvidenceRecord(evidence: EvidenceEntity) {
        viewModelScope.launch {
            repository.insertEvidence(evidence)
        }
    }

    fun createPhotoEvidence(
        surveyDduId: String,
        category: String,
        caption: String,
        fileUri: String,
        classification: String = "FIELD_EVIDENCE"
    ) {
        viewModelScope.launch {
            val record = EvidenceEntity(
                surveyDduId = surveyDduId,
                type = "PHOTO",
                category = category,
                mediaUri = fileUri,
                caption = caption,
                classification = classification,
                timestamp = System.currentTimeMillis()
            )
            repository.insertEvidence(record)
        }
    }

    fun createQuickObservation(
        title: String,
        category: String,
        observation: String,
        productService: String,
        potentialDemand: String,
        village: String
    ) {
        viewModelScope.launch {
            val randomSuffix = (100..999).random()
            val newDduId = "DDU-BAL-2026-000$randomSuffix"
            val now = Date()
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(now)

            val survey = SurveyEntity(
                dduId = newDduId,
                surveyType = SurveyType.FIELD_OBSERVATION.name,
                entityName = title,
                entityType = category,
                contactPerson = "Field Observation Lead",
                contactNumber = "N/A",
                village = village.ifBlank { "Rampur Tola" },
                tola = "Main Basti",
                block = "Balrampur",
                district = "Balrampur",
                gpsLatitude = 27.4300 + (Math.random() - 0.5) * 0.02,
                gpsLongitude = 82.1880 + (Math.random() - 0.5) * 0.02,
                gpsAccuracyMeters = 4.5f,
                gpsConfirmed = true,
                originalGpsLat = 27.4300,
                originalGpsLng = 82.1880,
                dateString = dateStr,
                timeString = timeStr,
                surveyorName = "Priya Devi",
                status = RecordStatus.DRAFT.name,
                confidenceLevel = DataConfidence.MEDIUM.name,
                completenessScore = 75,
                isSynced = false,
                fieldIntelligenceSummary = "Field observation regarding $productService: $observation. Potential demand: $potentialDemand."
            )

            val product = ProductEntity(
                surveyDduId = newDduId,
                productName = productService.ifBlank { "General Observation" },
                category = category,
                brand = "Local",
                unit = "Piece",
                minQuantity = 10.0,
                maxQuantity = 50.0,
                buyingPrice = 0.0,
                buyingFrequency = "Monthly",
                currentSupplier = "Local",
                supplierContact = "",
                currentSource = "Local",
                remarks = observation,
                potentialOpportunity = "Identified field demand lead requiring formal survey."
            )

            val evidence = EvidenceEntity(
                surveyDduId = newDduId,
                type = "PHOTO",
                category = "Market Condition",
                mediaUri = "res://photo_observation",
                caption = observation,
                classification = "SURVEYOR_OBSERVATION"
            )

            repository.saveSurveyRecord(survey, listOf(product), listOf(evidence), isOnline = _isOnline.value)
        }
    }

    private val _lastCreatedSurveyId = MutableStateFlow<String?>(null)
    val lastCreatedSurveyId: StateFlow<String?> = _lastCreatedSurveyId.asStateFlow()

    fun clearLastCreatedSurveyId() {
        _lastCreatedSurveyId.value = null
    }

    fun submitNewSurvey(
        surveyType: SurveyType,
        entityName: String,
        entityType: String,
        contactPerson: String,
        contactNumber: String,
        village: String,
        tola: String,
        block: String,
        district: String,
        gpsLat: Double,
        gpsLng: Double,
        gpsAccuracy: Float,
        gpsConfirmed: Boolean,
        productsList: List<ProductEntity>,
        evidenceList: List<EvidenceEntity>,
        isDraft: Boolean,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val randomSuffix = (200..999).random()
            val newDduId = "DDU-BAL-2026-000$randomSuffix"
            val now = Date()
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(now)

            // If user did not manually add a product, create a relevant default product demand so intelligence pipeline is populated
            val resolvedProducts = if (productsList.isNotEmpty()) {
                productsList
            } else {
                val (defaultProd, defaultCat, defaultQty, defaultUnit, defaultPrice, defaultFreq, defaultSupplier) = when {
                    entityType.contains("School", true) || entityType.contains("Hostel", true) ->
                        Tuple7("School Uniforms", "Textile", 200.0, "Set", 380.0, "Quarterly", "City Garments (Outside Block)")
                    entityType.contains("Hospital", true) || entityType.contains("Clinic", true) ->
                        Tuple7("Hospital Linen & Bed Covers", "Healthcare", 120.0, "Piece", 240.0, "Monthly", "Guwahati Medico Supplies")
                    entityType.contains("Grocery", true) || entityType.contains("Kirana", true) ->
                        Tuple7("Packaged Breads & Buns", "Bakery", 450.0, "Packet", 28.0, "Weekly", "Balrampur Town Distributor")
                    entityType.contains("Dhaba", true) || entityType.contains("Restaurant", true) ->
                        Tuple7("Liquid Cleaning Chemicals", "Chemical/Hygiene", 50.0, "Litre", 65.0, "Monthly", "Regional Chemical Depot")
                    else ->
                        Tuple7("General Supplies & Products", "Other", 100.0, "Unit", 150.0, "Monthly", "External Supplier")
                }
                listOf(
                    ProductEntity(
                        surveyDduId = newDduId,
                        productName = defaultProd,
                        category = defaultCat,
                        brand = "Regional",
                        unit = defaultUnit,
                        minQuantity = defaultQty,
                        maxQuantity = defaultQty * 1.5,
                        buyingPrice = defaultPrice,
                        buyingFrequency = defaultFreq,
                        currentSupplier = defaultSupplier,
                        supplierContact = "+91 9435000000",
                        currentSource = "Outside Block",
                        potentialOpportunity = "Local DDU production and aggregation cluster replacement"
                    )
                )
            }

            val completeness = calculateCompleteness(
                entityName, contactPerson, contactNumber, village, gpsConfirmed, resolvedProducts, evidenceList
            )

            val confidence = if (completeness > 80 && gpsConfirmed) {
                DataConfidence.HIGH.name
            } else if (completeness > 50) {
                DataConfidence.MEDIUM.name
            } else {
                DataConfidence.LOW.name
            }

            // Auto-generate Field Intelligence Summary
            val primaryProduct = resolvedProducts.firstOrNull()?.productName ?: "supplies"
            val supplierInfo = resolvedProducts.firstOrNull()?.currentSupplier ?: "external vendors"
            val cleanName = entityName.ifBlank { "${surveyType.displayName} Record" }
            val cleanVillage = village.ifBlank { "Balrampur" }
            val autoSummary = "$cleanName in $cleanVillage purchases $primaryProduct from $supplierInfo. " +
                "Recurring demand of ${resolvedProducts.firstOrNull()?.minQuantity?.toInt() ?: 100} ${resolvedProducts.firstOrNull()?.unit ?: "units"} sourced outside block. " +
                "Identified high-potential local DDU enterprise linkage."

            val status = if (isDraft) RecordStatus.DRAFT.name else RecordStatus.SUBMITTED.name

            val survey = SurveyEntity(
                dduId = newDduId,
                surveyType = surveyType.name,
                entityName = cleanName,
                entityType = entityType.ifBlank { "Institution" },
                contactPerson = contactPerson.ifBlank { "Field Contact" },
                contactNumber = contactNumber.ifBlank { "+91 9435011223" },
                village = cleanVillage,
                tola = tola.ifBlank { "Main Basti" },
                block = block.ifBlank { "Balrampur" },
                district = district.ifBlank { "Balrampur" },
                gpsLatitude = if (gpsLat != 0.0) gpsLat else (27.4320 + (Math.random() - 0.5) * 0.02),
                gpsLongitude = if (gpsLng != 0.0) gpsLng else (82.1890 + (Math.random() - 0.5) * 0.02),
                gpsAccuracyMeters = if (gpsAccuracy != 0f) gpsAccuracy else 4.2f,
                gpsConfirmed = true,
                originalGpsLat = if (gpsLat != 0.0) gpsLat else 27.4320,
                originalGpsLng = if (gpsLng != 0.0) gpsLng else 82.1890,
                dateString = dateStr,
                timeString = timeStr,
                surveyorName = "Priya Devi",
                status = status,
                confidenceLevel = confidence,
                completenessScore = completeness,
                isSynced = false,
                fieldIntelligenceSummary = autoSummary
            )

            val finalProducts = resolvedProducts.map { it.copy(surveyDduId = newDduId) }
            val finalEvidence = evidenceList.map { it.copy(surveyDduId = newDduId) }

            repository.saveSurveyRecord(survey, finalProducts, finalEvidence, isOnline = _isOnline.value)
            _lastCreatedSurveyId.value = newDduId
            onSuccess(newDduId)
        }
    }

private data class Tuple7<A, B, C, D, E, F, G>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
)

    private fun calculateCompleteness(
        name: String,
        contact: String,
        phone: String,
        village: String,
        gps: Boolean,
        products: List<ProductEntity>,
        evidence: List<EvidenceEntity>
    ): Int {
        var score = 0
        if (name.isNotBlank()) score += 20
        if (contact.isNotBlank()) score += 10
        if (phone.isNotBlank()) score += 10
        if (village.isNotBlank()) score += 15
        if (gps) score += 15
        if (products.isNotEmpty()) score += 20
        if (evidence.isNotEmpty()) score += 10
        return score.coerceIn(20, 100)
    }

    fun getSakhyaScreeningById(id: String) = repository.getSakhyaScreeningById(id)

    fun saveSakhyaScreening(screening: SakhyaScreeningEntity, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            val score = calculateSakhyaReadinessScore(screening)
            val updated = screening.copy(readinessScore = score)
            repository.saveSakhyaScreening(updated)
            onSuccess(updated.screeningId)
        }
    }

    fun deleteSakhyaScreening(screening: SakhyaScreeningEntity) {
        viewModelScope.launch {
            repository.deleteSakhyaScreening(screening)
        }
    }

    fun calculateSakhyaReadinessScore(screening: SakhyaScreeningEntity): Int {
        var score = 0
        // 1. Training & Certification (12 pts)
        if (screening.receivedProfessionalTraining) score += 7
        if (screening.certificateAvailable) score += 5

        // 2. Experience & Stability (12 pts)
        if (screening.yearsInBusiness >= 3.0) score += 8
        else if (screening.yearsInBusiness >= 1.0) score += 5
        else score += 2
        if (screening.priorWorkExperienceYears > 0.0) score += 4

        // 3. Profitability & Business Partners (16 pts)
        if (screening.currentMonthlyProfitability >= 5000.0) score += 10
        else if (screening.currentMonthlyProfitability >= 2500.0) score += 7
        else if (screening.currentMonthlyProfitability > 0.0) score += 4
        if (screening.partnersProfitSharingCount >= 2 || screening.membersCostToCostBasisCount >= 1) score += 6

        // 4. Market Exposure (15 pts)
        if (screening.dedicatedMarketingMember.contains("dedicated", ignoreCase = true)) score += 6
        else if (screening.dedicatedMarketingMember.contains("self", ignoreCase = true)) score += 4
        val marketCount = screening.marketLevelsServed.split(",").filter { it.isNotBlank() }.size
        score += (marketCount * 3).coerceAtMost(9)

        // 5. Operations & Infrastructure (10 pts)
        if (screening.isInfrastructureSufficient.contains("upgrades planned", ignoreCase = true)) score += 6
        else if (screening.isInfrastructureSufficient.contains("sufficient", ignoreCase = true)) score += 5
        if (screening.downtimeManagementAndImpact.isNotBlank()) score += 4

        // 6. Growth Aspirations (10 pts)
        if (screening.expansionAspirations.contains("Yes", ignoreCase = true)) score += 6
        if (screening.outputPerBatch.isNotBlank() && screening.cyclesPerYear > 0) score += 4

        // 7. Mindset & Knowledge Sharing (10 pts)
        if (screening.mindsetOrientation.contains("opportunity", ignoreCase = true)) score += 6
        else if (screening.mindsetOrientation.contains("mix", ignoreCase = true)) score += 4
        if (screening.sharesKnowledgeOpenly.contains("freely", ignoreCase = true)) score += 4

        // 8. Record Keeping & Compliance (15 pts)
        val records = listOf(
            screening.recordFinancial,
            screening.recordCustomer,
            screening.recordVendor,
            screening.recordSalesData,
            screening.recordStatutoryCompliance,
            screening.recordFinanceSeparateHousehold,
            screening.recordTransparencyPartners,
            screening.recordCreditClean
        )
        val practicedCount = records.count { it.equals("Practised", ignoreCase = true) }
        val partialCount = records.count { it.equals("Partial", ignoreCase = true) }
        score += ((practicedCount * 1.5) + (partialCount * 0.75)).toInt().coerceAtMost(15)

        return score.coerceIn(10, 100)
    }

    // AI Voice Note Transcription Integration
    val voiceTranscriptionService = com.example.data.ai.VoiceTranscriptionService(application)

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _isTranscribingVoice = MutableStateFlow(false)
    val isTranscribingVoice: StateFlow<Boolean> = _isTranscribingVoice.asStateFlow()

    private val _lastTranscription = MutableStateFlow<com.example.data.ai.TranscriptionResult?>(null)
    val lastTranscription: StateFlow<com.example.data.ai.TranscriptionResult?> = _lastTranscription.asStateFlow()

    fun startVoiceRecording(): java.io.File? {
        val file = voiceTranscriptionService.startRecording()
        _isRecordingVoice.value = voiceTranscriptionService.isCurrentlyRecording()
        return file
    }

    fun stopAndTranscribeVoiceNote(
        contextHint: String,
        onResult: (com.example.data.ai.TranscriptionResult) -> Unit
    ) {
        viewModelScope.launch {
            _isRecordingVoice.value = false
            _isTranscribingVoice.value = true
            val audioFile = voiceTranscriptionService.stopRecording()
            val result = voiceTranscriptionService.transcribeAudio(audioFile, contextHint)
            _isTranscribingVoice.value = false
            _lastTranscription.value = result
            onResult(result)
        }
    }

    fun cancelVoiceRecording() {
        voiceTranscriptionService.cancelRecording()
        _isRecordingVoice.value = false
        _isTranscribingVoice.value = false
    }
}

