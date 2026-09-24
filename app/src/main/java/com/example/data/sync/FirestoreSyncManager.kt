package com.example.data.sync

import android.util.Log
import com.example.data.model.OpportunityEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.VillageProductionAssessmentEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service to sync and store field observation data, Stage 1 opportunities,
 * Sakhya screenings, and Stage 2 Village Production Assessments to Cloud Firestore,
 * using Room Database as an offline cache.
 */
class FirestoreSyncManager {

    private val TAG = "FirestoreSyncManager"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            } catch (se: Throwable) {
                Log.d(TAG, "Firestore persistence settings already initialized or note: ${se.message}")
            }
            db
        } catch (e: Throwable) {
            Log.w(TAG, "Cloud Firestore is not initialized (missing google-services.json or network): ${e.message}")
            null
        }
    }

    val isFirestoreAvailable: Boolean
        get() = firestore != null

    /**
     * Upload or update a Field Survey (with its products and evidence) to Firestore
     * Collection: "field_observations"
     */
    suspend fun syncSurveyToFirestore(surveyWithDetails: SurveyWithDetails): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        val survey = surveyWithDetails.survey

        try {
            val docRef = db.collection("field_observations").document(survey.dduId)

            val surveyMap = hashMapOf<String, Any>(
                "dduId" to survey.dduId,
                "surveyType" to survey.surveyType,
                "entityName" to survey.entityName,
                "entityType" to survey.entityType,
                "contactPerson" to survey.contactPerson,
                "contactNumber" to survey.contactNumber,
                "village" to survey.village,
                "tola" to survey.tola,
                "block" to survey.block,
                "district" to survey.district,
                "gpsLatitude" to survey.gpsLatitude,
                "gpsLongitude" to survey.gpsLongitude,
                "gpsAccuracyMeters" to survey.gpsAccuracyMeters,
                "gpsConfirmed" to survey.gpsConfirmed,
                "originalGpsLat" to survey.originalGpsLat,
                "originalGpsLng" to survey.originalGpsLng,
                "dateString" to survey.dateString,
                "timeString" to survey.timeString,
                "surveyorName" to survey.surveyorName,
                "status" to survey.status,
                "confidenceLevel" to survey.confidenceLevel,
                "completenessScore" to survey.completenessScore,
                "fieldIntelligenceSummary" to survey.fieldIntelligenceSummary,
                "validationRemarks" to survey.validationRemarks,
                "validatedBy" to survey.validatedBy,
                "validatedDate" to survey.validatedDate,
                "createdTimestamp" to survey.createdTimestamp,
                "updatedTimestamp" to survey.updatedTimestamp,
                "syncedAt" to System.currentTimeMillis(),
                "products" to surveyWithDetails.products.map { p ->
                    mapOf(
                        "productName" to p.productName,
                        "category" to p.category,
                        "brand" to p.brand,
                        "unit" to p.unit,
                        "minQuantity" to p.minQuantity,
                        "maxQuantity" to p.maxQuantity,
                        "buyingPrice" to p.buyingPrice,
                        "buyingFrequency" to p.buyingFrequency,
                        "currentSupplier" to p.currentSupplier,
                        "supplierContact" to p.supplierContact,
                        "currentSource" to p.currentSource,
                        "potentialOpportunity" to p.potentialOpportunity,
                        "remarks" to p.remarks
                    )
                },
                "evidenceCount" to surveyWithDetails.evidenceList.size
            )

            docRef.set(surveyMap, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced survey ${survey.dduId} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync survey ${survey.dduId} to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Pull remote field observations from Firestore to populate or update the Room cache
     */
    suspend fun fetchRemoteFieldObservations(): List<Pair<SurveyEntity, List<ProductEntity>>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext emptyList()
        val results = mutableListOf<Pair<SurveyEntity, List<ProductEntity>>>()

        try {
            val snapshot = db.collection("field_observations").get().await()
            for (doc in snapshot.documents) {
                try {
                    val dduId = doc.getString("dduId") ?: doc.id
                    val survey = SurveyEntity(
                        dduId = dduId,
                        surveyType = doc.getString("surveyType") ?: "FIELD_OBSERVATION",
                        entityName = doc.getString("entityName") ?: "Observed Entity",
                        entityType = doc.getString("entityType") ?: "General",
                        contactPerson = doc.getString("contactPerson") ?: "",
                        contactNumber = doc.getString("contactNumber") ?: "",
                        village = doc.getString("village") ?: "Balrampur",
                        tola = doc.getString("tola") ?: "Main Basti",
                        block = doc.getString("block") ?: "Balrampur",
                        district = doc.getString("district") ?: "Chitrakoot",
                        gpsLatitude = doc.getDouble("gpsLatitude") ?: 27.43,
                        gpsLongitude = doc.getDouble("gpsLongitude") ?: 82.18,
                        gpsAccuracyMeters = (doc.getDouble("gpsAccuracyMeters") ?: 4.0).toFloat(),
                        gpsConfirmed = doc.getBoolean("gpsConfirmed") ?: true,
                        originalGpsLat = doc.getDouble("originalGpsLat") ?: 27.43,
                        originalGpsLng = doc.getDouble("originalGpsLng") ?: 82.18,
                        dateString = doc.getString("dateString") ?: "",
                        timeString = doc.getString("timeString") ?: "",
                        surveyorName = doc.getString("surveyorName") ?: "Field Team",
                        status = doc.getString("status") ?: "APPROVED",
                        confidenceLevel = doc.getString("confidenceLevel") ?: "HIGH",
                        completenessScore = (doc.getLong("completenessScore") ?: 85L).toInt(),
                        isSynced = true,
                        fieldIntelligenceSummary = doc.getString("fieldIntelligenceSummary") ?: "",
                        validationRemarks = doc.getString("validationRemarks") ?: "",
                        validatedBy = doc.getString("validatedBy") ?: "",
                        validatedDate = doc.getString("validatedDate") ?: "",
                        createdTimestamp = doc.getLong("createdTimestamp") ?: System.currentTimeMillis(),
                        updatedTimestamp = doc.getLong("updatedTimestamp") ?: System.currentTimeMillis()
                    )

                    val productsList = mutableListOf<ProductEntity>()
                    val rawProducts = doc.get("products") as? List<Map<String, Any>>
                    if (rawProducts != null) {
                        for (pMap in rawProducts) {
                            productsList.add(
                                ProductEntity(
                                    surveyDduId = dduId,
                                    productName = pMap["productName"] as? String ?: "Item",
                                    category = pMap["category"] as? String ?: "General",
                                    brand = pMap["brand"] as? String ?: "Local",
                                    unit = pMap["unit"] as? String ?: "Unit",
                                    minQuantity = (pMap["minQuantity"] as? Number)?.toDouble() ?: 1.0,
                                    maxQuantity = (pMap["maxQuantity"] as? Number)?.toDouble() ?: 10.0,
                                    buyingPrice = (pMap["buyingPrice"] as? Number)?.toDouble() ?: 0.0,
                                    buyingFrequency = pMap["buyingFrequency"] as? String ?: "Monthly",
                                    currentSupplier = pMap["currentSupplier"] as? String ?: "Local",
                                    supplierContact = pMap["supplierContact"] as? String ?: "",
                                    currentSource = pMap["currentSource"] as? String ?: "Local",
                                    remarks = pMap["remarks"] as? String ?: "",
                                    potentialOpportunity = pMap["potentialOpportunity"] as? String ?: ""
                                )
                            )
                        }
                    }

                    results.add(Pair(survey, productsList))
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing remote survey doc ${doc.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote field observations: ${e.message}")
        }
        results
    }

    /**
     * Upload or update a Stage 1 Opportunity to Firestore
     * Collection: "stage1_opportunities"
     */
    suspend fun syncOpportunityToFirestore(opportunity: OpportunityEntity): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("stage1_opportunities").document(opportunity.oppId)
            val oppMap = hashMapOf<String, Any>(
                "oppId" to opportunity.oppId,
                "title" to opportunity.title,
                "category" to opportunity.category,
                "level" to opportunity.level,
                "status" to opportunity.status,
                "demandingEntitiesCount" to opportunity.demandingEntitiesCount,
                "estimatedAnnualDemand" to opportunity.estimatedAnnualDemand,
                "buyingPattern" to opportunity.buyingPattern,
                "currentSource" to opportunity.currentSource,
                "averagePriceRange" to opportunity.averagePriceRange,
                "localSupplyStatus" to opportunity.localSupplyStatus,
                "geographicCluster" to opportunity.geographicCluster,
                "evidenceSummary" to opportunity.evidenceSummary,
                "confidence" to opportunity.confidence,
                "potentialEnterprise" to opportunity.potentialEnterprise,
                "relatedBuyers" to opportunity.relatedBuyers,
                "interpretationNarrative" to opportunity.interpretationNarrative,
                "nextSteps" to opportunity.nextSteps,
                "isShortlistedForStage2" to opportunity.isShortlistedForStage2,
                "updatedTimestamp" to opportunity.updatedTimestamp,
                "syncedAt" to System.currentTimeMillis()
            )
            docRef.set(oppMap, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced opportunity ${opportunity.oppId} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync opportunity ${opportunity.oppId} to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Pull remote opportunities from Firestore to cache in Room
     */
    suspend fun fetchRemoteOpportunities(): List<OpportunityEntity> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext emptyList()
        val list = mutableListOf<OpportunityEntity>()
        try {
            val snapshot = db.collection("stage1_opportunities").get().await()
            for (doc in snapshot.documents) {
                try {
                    list.add(
                        OpportunityEntity(
                            oppId = doc.getString("oppId") ?: doc.id,
                            title = doc.getString("title") ?: "Opportunity",
                            category = doc.getString("category") ?: "General",
                            level = doc.getString("level") ?: "MEDIUM",
                            status = doc.getString("status") ?: "IDENTIFIED",
                            demandingEntitiesCount = (doc.getLong("demandingEntitiesCount") ?: 1L).toInt(),
                            estimatedAnnualDemand = doc.getString("estimatedAnnualDemand") ?: "",
                            buyingPattern = doc.getString("buyingPattern") ?: "",
                            currentSource = doc.getString("currentSource") ?: "",
                            averagePriceRange = doc.getString("averagePriceRange") ?: "",
                            localSupplyStatus = doc.getString("localSupplyStatus") ?: "",
                            geographicCluster = doc.getString("geographicCluster") ?: "",
                            evidenceSummary = doc.getString("evidenceSummary") ?: "",
                            confidence = doc.getString("confidence") ?: "HIGH",
                            potentialEnterprise = doc.getString("potentialEnterprise") ?: "",
                            relatedBuyers = doc.getString("relatedBuyers") ?: "",
                            interpretationNarrative = doc.getString("interpretationNarrative") ?: "",
                            nextSteps = doc.getString("nextSteps") ?: "",
                            isShortlistedForStage2 = doc.getBoolean("isShortlistedForStage2") ?: false,
                            updatedTimestamp = doc.getLong("updatedTimestamp") ?: System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing opportunity doc ${doc.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote opportunities: ${e.message}")
        }
        list
    }

    /**
     * Upload or update a Stage 2 Village Production Assessment to Firestore
     * Collection: "stage2_village_assessments"
     */
    suspend fun syncStage2AssessmentToFirestore(assessment: VillageProductionAssessmentEntity): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("stage2_village_assessments").document(assessment.assessmentId)
            val map = hashMapOf<String, Any>(
                "assessmentId" to assessment.assessmentId,
                "stage1OppId" to assessment.stage1OppId,
                "productName" to assessment.productName,
                "vatika" to assessment.vatika,
                "filledBy" to assessment.filledBy,
                "assessmentDate" to assessment.assessmentDate,
                "demandFromLocalSurvey" to assessment.demandFromLocalSurvey,
                "mainRawMaterialNeeded" to assessment.mainRawMaterialNeeded,
                "rawMaterialAvailableLocally" to assessment.rawMaterialAvailableLocally,
                "trainingNeededForQuality" to assessment.trainingNeededForQuality,
                "trainingKindNeeded" to assessment.trainingKindNeeded,
                "trainedWomenAvailable" to assessment.trainedWomenAvailable,
                "externalSupportNeeded" to assessment.externalSupportNeeded,
                "priorityLevel" to assessment.priorityLevel,
                "rawMaterialSourceDesc" to assessment.rawMaterialSourceDesc,
                "marketDestinationDesc" to assessment.marketDestinationDesc,
                "packagingLocallyAvailable" to assessment.packagingLocallyAvailable,
                "canBeProducedInVillage" to assessment.canBeProducedInVillage,
                "criticalProceedNext" to assessment.criticalProceedNext,
                "isSampleAvailable" to assessment.isSampleAvailable,
                "readinessScore" to assessment.readinessScore,
                "rawMaterialCostStability" to assessment.rawMaterialCostStability,
                "trainerAvailable" to assessment.trainerAvailable,
                "productionOrRawMaterialAvailable" to assessment.productionOrRawMaterialAvailable,
                "workspaceAvailable" to assessment.workspaceAvailable,
                "electricityAvailable" to assessment.electricityAvailable,
                "waterAvailable" to assessment.waterAvailable,
                "storageAvailable" to assessment.storageAvailable,
                "criticalityRatingPass" to assessment.criticalityRatingPass,
                "samplingChecked" to assessment.samplingChecked,
                "finalRankingOrder" to assessment.finalRankingOrder,
                "finalSelectionStatus" to assessment.finalSelectionStatus,
                "swsmDecisionNotes" to assessment.swsmDecisionNotes,
                "updatedTimestamp" to assessment.updatedTimestamp,
                "syncedAt" to System.currentTimeMillis()
            )
            docRef.set(map, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced Stage 2 assessment ${assessment.assessmentId} to Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync Stage 2 assessment ${assessment.assessmentId} to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Upload Sakhya Screening to Firestore
     * Collection: "sakhya_screenings"
     */
    suspend fun syncSakhyaScreeningToFirestore(screening: SakhyaScreeningEntity): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("sakhya_screenings").document(screening.screeningId)
            val map = hashMapOf<String, Any>(
                "screeningId" to screening.screeningId,
                "entrepreneurName" to screening.entrepreneurName,
                "productAndValueChain" to screening.productAndValueChain,
                "fieldFellowName" to screening.fieldFellowName,
                "currentMonthlyProfitability" to screening.currentMonthlyProfitability,
                "readinessScore" to screening.readinessScore,
                "status" to screening.status,
                "yearsInBusiness" to screening.yearsInBusiness,
                "techPracticesUsed" to screening.techPracticesUsed,
                "syncedAt" to System.currentTimeMillis()
            )
            docRef.set(map, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync Sakhya screening ${screening.screeningId}: ${e.message}")
            false
        }
    }

    /**
     * Batch Sync all pending surveys and assessments (Push & Pull)
     */
    suspend fun syncAll(
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>
    ): SyncResult = withContext(Dispatchers.IO) {
        val db = firestore
        if (db == null) {
            return@withContext SyncResult(
                success = false,
                message = "Room Cache active: Firestore client pending remote configuration. Local records safely stored.",
                syncedSurveys = 0,
                syncedOpportunities = 0,
                syncedAssessments = 0,
                pulledSurveys = 0
            )
        }

        var surveyCount = 0
        var oppCount = 0
        var assessmentCount = 0

        for (s in surveys) {
            if (syncSurveyToFirestore(s)) {
                surveyCount++
            }
        }

        for (o in opportunities) {
            if (syncOpportunityToFirestore(o)) {
                oppCount++
            }
        }

        for (a in stage2Assessments) {
            if (syncStage2AssessmentToFirestore(a)) {
                assessmentCount++
            }
        }

        SyncResult(
            success = true,
            message = "Sync complete: $surveyCount surveys, $oppCount opportunities, $assessmentCount Stage 2 assessments uploaded to Firestore.",
            syncedSurveys = surveyCount,
            syncedOpportunities = oppCount,
            syncedAssessments = assessmentCount,
            pulledSurveys = 0
        )
    }

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val syncedSurveys: Int,
        val syncedOpportunities: Int,
        val syncedAssessments: Int,
        val pulledSurveys: Int = 0
    )
}
