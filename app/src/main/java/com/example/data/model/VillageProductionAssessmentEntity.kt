package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stage 2 · Village Production Assessment
 * "Can our village produce this, competitively?"
 * Work through Part A to Part E in order, then record the SWSM decision at the end.
 */
@Entity(
    tableName = "stage2_village_assessments",
    indices = [
        Index(value = ["assessmentId"], unique = true),
        Index(value = ["productName"]),
        Index(value = ["vatika"]),
        Index(value = ["finalSelectionStatus"])
    ]
)
data class VillageProductionAssessmentEntity(
    @PrimaryKey val assessmentId: String, // e.g. "VPA-2026-001"
    val stage1OppId: String = "",        // Linked Stage 1 Opportunity ID e.g. "OPP-024"
    val productName: String,             // Product Being Assessed
    val vatika: String = "Balrampur Central", // Vatika / Village / Cluster Name
    val filledBy: String = "Field Fellow / DIC Team",
    val assessmentDate: String = "2026-09-24",

    // Part A — Basic Information
    val demandFromLocalSurvey: Boolean = true, // Whether Demand of product is there from Local Market Survey (Yes/No)
    val mainRawMaterialNeeded: String = "",    // Main raw material needed
    val rawMaterialAvailableLocally: Boolean = true, // Whether Raw Material available (within Village & can be purchased from Local producer/Farmer) – Yes / No
    val trainingNeededForQuality: Boolean = true,    // Training needed for Quality? (Y/N)
    val trainingKindNeeded: String = "",             // what kind?
    val trainedWomenAvailable: Boolean = true,       // Whether trained women/Vaibhavi are available? (Yes/No)
    val externalSupportNeeded: String = "",          // External support needed (trainer, branding, licence…)

    // Part B — Priority Level (1 to 6)
    // Priority is set by where the raw material comes from and where the product will be sold.
    // 1: Purchased from Producer/Farmer | Local
    // 2: Purchased from Local Market | Local
    // 3: Outside of Local/nearest Market | Local
    // 4: Purchased from Producer/Farmer | Outside of Vatika/nearby market
    // 5: Purchased from Local Market | Outside of Vatika/nearby market
    // 6: Outside of Local/nearest Market | Outside of Vatika/nearby market
    val priorityLevel: Int = 1,
    val rawMaterialSourceDesc: String = "Purchased from Producer/Farmer",
    val marketDestinationDesc: String = "Local",

    // Part C — Critical (Mandatory)
    // All two must Yes. A single No makes the product Not Suitable, regardless of the score in Part B.
    val packagingLocallyAvailable: Boolean = true, // Packaging locally available, or can be purchased by SWSM from outside/nearby market (within comfortable range)
    val canBeProducedInVillage: Boolean = true,    // Can this be produced in the village? (infrastructure/cultural acceptance/affordability/women producers available)
    val criticalProceedNext: Boolean = true,       // All Yes = Proceed Next, Any No = Do Not Proceed

    // Part D — Sample Availability Check
    // Whether Product is being produced locally & sample is available? No = 0 | Yes = 1
    // If Product Sample is ready, proceed directly for DDU Designing (Stage 3), if Not, Check for Readiness (Part E)
    val isSampleAvailable: Boolean = false,

    // Part E — Readiness Check (Score each factor Rating 0 or 1, Total out of 7)
    val rawMaterialCostStability: Int = 1,          // 0 = A lot of fluctuation, 1 = Sometimes/Stable
    val trainerAvailable: Int = 1,                  // 0 = No, 1 = Yes
    val productionOrRawMaterialAvailable: Int = 1,  // 0 = No, 1 = Yes
    val workspaceAvailable: Int = 1,                // 0 = Not Available, 1 = Available
    val electricityAvailable: Int = 1,              // 0 = Required and Not Available, 1 = Not Required and/or Available
    val waterAvailable: Int = 1,                    // 0 = Required and Not Available, 1 = Not Required and/or Available
    val storageAvailable: Int = 1,                  // 0 = Required and Not Available, 1 = Not Required and/or Available
    val readinessScore: Int = 7,                    // Rating count out of 7 (0 to 7)

    // Final Product List for DDU Selection (Synthesis & Decision)
    val criticalityRatingPass: Boolean = true,      // Pass / Fail
    val samplingChecked: Boolean = false,           // Yes / No
    val finalRankingOrder: Int = 1,                 // 1 = Highest priority for DDU selection
    val finalSelectionStatus: String = "Selected for Stage 3", // "Selected for Stage 3 (Direct Sample)", "Selected for Stage 3 (High Readiness)", "Suitable with Support", "Dropped (Criticality Fail)"
    val swsmDecisionNotes: String = "",
    val isSyncedToFirestore: Boolean = false,
    val updatedTimestamp: Long = System.currentTimeMillis()
)
