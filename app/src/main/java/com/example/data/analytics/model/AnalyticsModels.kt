package com.example.data.analytics.model

import com.example.data.model.DataConfidence
import com.example.data.model.OpportunityStage

/**
 * Stage History record tracking time spent and transition per DDU stage
 */
data class StageHistoryRecord(
    val id: String,
    val dduId: String,
    val stageName: String,
    val stageOrder: Int,
    val enteredDate: String,
    val completedDate: String? = null,
    val daysSpent: Int = 0,
    val status: String = "COMPLETED", // COMPLETED, IN_PROGRESS, PENDING, DROPPED_OUT
    val transitionRemarks: String = "",
    val verifiedBy: String = ""
)

/**
 * Dynamic Executive Dashboard KPI summary
 */
data class ExecutiveKpiSummary(
    val totalDdus: Int = 0,
    val activeDdus: Int = 0,
    val ddusInStage1: Int = 0,
    val ddusInStage2: Int = 0,
    val ddusInStage3: Int = 0,
    val completedDdus: Int = 0,
    val institutionalSurveysCompleted: Int = 0,
    val retailSurveysCompleted: Int = 0,
    val businessesMapped: Int = 0,
    val vendorsIdentified: Int = 0,
    val productsIdentified: Int = 0,
    val opportunitiesShortlisted: Int = 0,
    val swsmGroupsLinked: Int = 0,
    val vaibhaviEntrepreneursLinked: Int = 0,
    val potentialSakhyasIdentified: Int = 0,
    val fieldVisitsCompleted: Int = 0,
    val villagesCovered: Int = 0,
    val marketsSurveyed: Int = 0,
    val institutionsSurveyed: Int = 0,
    val estimatedLocalDemandValueMonthly: Double = 0.0,
    val estimatedLocalSupplyCapacityMonthly: Double = 0.0,
    val marketGapsIdentified: Int = 0
)

/**
 * Filter parameters for cross-filtering Power BI analytics
 */
data class AnalyticsFilterState(
    val state: String = "ALL",
    val district: String = "ALL",
    val block: String = "ALL",
    val village: String = "ALL",
    val vaatika: String = "ALL",
    val dduId: String = "ALL",
    val dduStage: String = "ALL",
    val productCategory: String = "ALL",
    val institutionType: String = "ALL",
    val confidenceLevel: String = "ALL",
    val opportunityStatus: String = "ALL",
    val searchQuery: String = "",
    val pilotModeOnly: Boolean = false
)

/**
 * DDU Pipeline Stage Metric
 */
data class PipelineStageMetric(
    val stageKey: String,
    val stageTitle: String,
    val stageOrder: Int,
    val countEntering: Int,
    val countProgressing: Int,
    val countPending: Int,
    val countRejected: Int,
    val averageDaysSpent: Double,
    val conversionPercentage: Double,
    val dropOffPercentage: Double,
    val associatedDduIds: List<String> = emptyList()
)

/**
 * Product Opportunity Item with deep analytics
 */
data class ProductOpportunityMetric(
    val productName: String,
    val category: String,
    val institutionsDemandingCount: Int,
    val retailersDemandingCount: Int,
    val villagesCount: Int,
    val averageMonthlyQuantity: Double,
    val unit: String,
    val averagePurchasePrice: Double,
    val lowestRecordedPrice: Double,
    val highestRecordedPrice: Double,
    val currentSupplierLocation: String,
    val isExternalSupplier: Boolean,
    val localSuppliersAvailableCount: Int,
    val swsmGroupsCapableCount: Int,
    val vaibhavisCapableCount: Int,
    val estimatedMonthlyOpportunityValue: Double,
    val estimatedAnnualOpportunityValue: Double,
    val demandSupplyGapQuantity: Double,
    val confidence: String,
    val opportunityScore: Int,
    val scoreCategory: String, // High Potential, Moderate Potential, Needs Validation, Insufficient Data
    val associatedDduId: String? = null,
    val sampleStatus: String = "Not Initiated"
)

/**
 * Calculated DDU Opportunity Score evaluation
 */
data class OpportunityScoreResult(
    val score: Int, // 0 to 100
    val category: String, // High Potential, Moderate Potential, Needs Validation, Insufficient Data
    val breakdownFactors: Map<String, Int>,
    val explanationText: String,
    val recommendationText: String
)

/**
 * Demand vs Supply Gap Model
 */
data class MarketGapItem(
    val productName: String,
    val category: String,
    val localDemandQuantity: Double,
    val existingLocalSupplyQuantity: Double,
    val externalSupplyQuantity: Double,
    val unmetDemandQuantity: Double,
    val potentialLocalProductionCapacity: Double,
    val unit: String,
    val isLocalisationOpportunity: Boolean,
    val estimatedMonthlyRevenuePotential: Double,
    val primaryProcurementOrigin: String
)

/**
 * Institutional Demand Table Row
 */
data class InstitutionalDemandRow(
    val institutionName: String,
    val institutionType: String,
    val village: String,
    val block: String,
    val productName: String,
    val monthlyRequirement: String,
    val purchaseFrequency: String,
    val currentSupplier: String,
    val supplierLocation: String,
    val unitPrice: Double,
    val procurementMethod: String,
    val willingnessToSourceLocally: String,
    val sampleStatus: String, // None, Requested, Submitted, Accepted, Rejected
    val potentialLocalSupplierOrSWSM: String,
    val status: String,
    val surveyDduId: String
)

/**
 * SWSM Producer Group Analytics Profile
 */
data class SwsmGroupProfile(
    val id: String,
    val name: String,
    val village: String,
    val block: String,
    val memberCount: Int,
    val currentProductsMade: List<String>,
    val skillsAvailable: List<String>,
    val equipmentAvailable: List<String>,
    val trainingCompleted: String,
    val monthlyProductionCapacity: String,
    val currentSalesChannels: String,
    val potentialProducts: List<String>,
    val linkedDdusCount: Int,
    val linkedInstitutionsCount: Int,
    val linkedRetailersCount: Int,
    val samplesRequestedCount: Int,
    val estimatedMonthlyRevenueOpportunity: Double
)

/**
 * Vaibhavi Entrepreneur Profile
 */
data class VaibhaviEntrepreneurProfile(
    val id: String,
    val name: String,
    val village: String,
    val businessType: String,
    val primaryProduct: String,
    val currentMonthlyProduction: String,
    val productionCapacity: String,
    val monthlySales: Double,
    val equipment: String,
    val skills: String,
    val trainingNeeded: String,
    val hasDigitalPayment: Boolean,
    val packagingCapability: String,
    val linkedDduId: String? = null,
    val potentialOpportunityTitle: String = ""
)

/**
 * Sakhya & Trainer Profile
 */
data class SakhyaTrainerProfile(
    val id: String,
    val name: String,
    val sectorSkill: String, // Poultry, Textiles, Soap, Agarbatti, Food, etc.
    val village: String,
    val trainingRadiusKm: Int,
    val contactStatus: String,
    val trainingAvailability: String,
    val capacityToTrainWomen: Int,
    val pastExperienceYears: Int,
    val linkedDduId: String? = null,
    val linkedSwsmGroup: String? = null
)

/**
 * Data Quality and Completeness metric per DDU record
 */
data class DduDataQualityMetric(
    val dduId: String,
    val entityName: String,
    val village: String,
    val completenessScore: Int,
    val hasGps: Boolean,
    val hasPhotos: Boolean,
    val hasContacts: Boolean,
    val hasSupplierInfo: Boolean,
    val hasDemandPricing: Boolean,
    val isSupervisorVerified: Boolean,
    val isPotentiallyDuplicate: Boolean,
    val issuesList: List<String>,
    val statusText: String
)

/**
 * Vaatika Analytics Profile
 */
data class VaatikaAnalyticsProfile(
    val vaatikaName: String,
    val territory: String,
    val totalVillages: Int,
    val totalHouseholdsEstimate: Int,
    val swsmMembersCount: Int,
    val vaibhavisCount: Int,
    val sakhyasCount: Int,
    val ddusCount: Int,
    val fieldVisitsCount: Int,
    val businessesCount: Int,
    val institutionsCount: Int,
    val shortlistedProductsCount: Int,
    val stage2ProductsCount: Int,
    val samplesRequestedCount: Int,
    val topOpportunities: List<String>
)

/**
 * Weekly Review Report metrics
 */
data class WeeklyReviewMetrics(
    val weekLabel: String,
    val fieldVisitsCompleted: Int,
    val ddusWorkedOn: Int,
    val newDdusInitiated: Int,
    val stageMovementsCount: Int,
    val productsShortlisted: Int,
    val institutionsVisited: Int,
    val businessesMapped: Int,
    val swsmMeetingsCount: Int,
    val vaibhaviInteractionsCount: Int,
    val sakhyaProspectsIdentified: Int,
    val samplesRequestedCount: Int,
    val samplesDeliveredCount: Int,
    val majorFindings: List<String>,
    val pendingValidations: List<String>,
    val blockers: List<String>,
    val nextActions: List<String>
)

/**
 * Power BI Embed Configuration & Authentication State
 */
data class PowerBiConfig(
    val workspaceId: String = "ws-ddu-analytics-prod",
    val reportId: String = "rep-ddu-exec-01",
    val datasetId: String = "sem-ddu-intelligence-v3",
    val tenantId: String = "72f988bf-86f1-41af-91ab-2d7cd011db47",
    val clientId: String = "app-ddu-bi-client-01",
    val isConfigured: Boolean = false,
    val embedUrl: String = "https://app.powerbi.com/reportEmbed?reportId=rep-ddu-exec-01&autoAuth=true&ctid=72f988bf-86f1-41af-91ab-2d7cd011db47",
    val lastRefreshTime: String = "Just now",
    val refreshStatus: String = "ONLINE_SYNCED",
    val autoRefreshMinutes: Int = 30
)
