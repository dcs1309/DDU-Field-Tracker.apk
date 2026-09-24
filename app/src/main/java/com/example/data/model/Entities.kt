package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "surveys",
    indices = [
        Index(value = ["dduId"], unique = true),
        Index(value = ["village"]),
        Index(value = ["status"])
    ]
)
data class SurveyEntity(
    @PrimaryKey val dduId: String, // e.g. DDU-BAL-2026-000124
    val surveyType: String, // INSTITUTION, LOCAL_SHOP, SUPPLIER, CUSTOMER_LEAD, FIELD_OBSERVATION
    val entityName: String,
    val entityType: String,
    val contactPerson: String,
    val contactNumber: String,
    val village: String,
    val tola: String,
    val block: String,
    val district: String,
    val gpsLatitude: Double,
    val gpsLongitude: Double,
    val gpsAccuracyMeters: Float,
    val gpsConfirmed: Boolean,
    val originalGpsLat: Double,
    val originalGpsLng: Double,
    val dateString: String,
    val timeString: String,
    val surveyorName: String,
    val status: String, // DRAFT, PENDING_SYNC, SUBMITTED, UNDER_REVIEW, RETURNED, APPROVED, REJECTED, DUPLICATE
    val confidenceLevel: String, // HIGH, MEDIUM, LOW
    val completenessScore: Int, // 0 to 100
    val isSynced: Boolean,
    val fieldIntelligenceSummary: String,
    val validationRemarks: String = "",
    val validatedBy: String = "",
    val validatedDate: String = "",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = SurveyEntity::class,
            parentColumns = ["dduId"],
            childColumns = ["surveyDduId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["surveyDduId"]),
        Index(value = ["productName"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyDduId: String,
    val productName: String,
    val category: String, // Textile, Bakery, Snacks/Kirana, Chemical/Hygiene, Healthcare, Agri, Other
    val brand: String, // Local, Branded, Unknown
    val unit: String, // Piece, Kg, Litre, Packet, Set, Dozen
    val minQuantity: Double,
    val maxQuantity: Double,
    val buyingPrice: Double,
    val mrp: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val buyingFrequency: String, // Daily, Weekly, Monthly, Quarterly, Yearly, Seasonal, As Required
    val currentSupplier: String,
    val supplierContact: String,
    val currentSource: String, // Local, Outside Village, Outside Block, Outside District, Unknown
    val seasonalDemand: Boolean = false,
    val peakMonths: String = "",
    val shopsSellingCount: Int = 0,
    val weeklySalesQty: Double = 0.0,
    val monthlySalesQty: Double = 0.0,
    val remarks: String = "",
    val potentialOpportunity: String = ""
)

@Entity(
    tableName = "evidence",
    foreignKeys = [
        ForeignKey(
            entity = SurveyEntity::class,
            parentColumns = ["dduId"],
            childColumns = ["surveyDduId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["surveyDduId"]),
        Index(value = ["type"])
    ]
)
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyDduId: String,
    val productId: Long? = null,
    val type: String, // PHOTO, VIDEO, VOICE_NOTE, DOCUMENT
    val category: String, // Institution, Shopfront, Product, Packaging, Price, Bill/Invoice, Existing Stock, Supplier, etc.
    val mediaUri: String,
    val caption: String,
    val transcription: String? = null, // for voice notes
    val durationSeconds: Int = 0, // for audio/video
    val classification: String = "FIELD_EVIDENCE", // FIELD_EVIDENCE, RESPONDENT_INFORMATION, SURVEYOR_OBSERVATION, DOCUMENTARY_EVIDENCE
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["supplierName"]),
        Index(value = ["location"])
    ]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierName: String,
    val supplierType: String, // Local, External, Distributor, Wholesaler, Tailor
    val contactNumber: String,
    val location: String,
    val productsSupplied: String,
    val priceTerms: String,
    val minimumOrderQuantity: String,
    val deliveryTerms: String,
    val supplyFrequency: String,
    val isLocal: Boolean,
    val remarks: String = ""
)

@Entity(
    tableName = "opportunities",
    indices = [
        Index(value = ["oppId"], unique = true),
        Index(value = ["status"])
    ]
)
data class OpportunityEntity(
    @PrimaryKey val oppId: String, // e.g. OPP-024
    val title: String, // e.g. School Uniforms
    val category: String, // Textile / Stitching
    val level: String, // HIGH, MEDIUM, LOW, UNDETERMINED
    val status: String, // IDENTIFIED, EVIDENCE_COLLECTED, PATTERN_CONFIRMED, VALIDATION_REQUIRED, FEASIBILITY_STUDY, PILOT_OPPORTUNITY, ACTIVE_OPPORTUNITY
    val demandingEntitiesCount: Int,
    val estimatedAnnualDemand: String,
    val buyingPattern: String,
    val currentSource: String,
    val averagePriceRange: String,
    val localSupplyStatus: String,
    val geographicCluster: String,
    val evidenceSummary: String,
    val confidence: String,
    val potentialEnterprise: String,
    val relatedBuyers: String,
    val interpretationNarrative: String,
    val nextSteps: String,
    val responsiblePerson: String = "Block Enterprise Coordinator",
    val followUpDate: String = "30 Sep 2026",
    val isShortlistedForStage2: Boolean = false,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_aliases")
data class ProductAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val standardName: String,
    val aliasName: String
)
