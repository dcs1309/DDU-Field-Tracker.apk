package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing the DSDC — SAKHYA-UDYAMI PROGRAMME
 * Sakhya Prospect Screening Form (Form F1, All value chains, Field fellow / DIC team).
 */
@Entity(
    tableName = "sakhya_screenings",
    indices = [
        Index(value = ["screeningId"], unique = true),
        Index(value = ["entrepreneurName"]),
        Index(value = ["productAndValueChain"]),
        Index(value = ["status"])
    ]
)
data class SakhyaScreeningEntity(
    @PrimaryKey val screeningId: String, // e.g. "SKH-2026-001"
    val linkedDduId: String = "",        // Linked survey if available (e.g. DDU-BAL-2026-000124)

    // I. Name of Vaibhavi / MEG / Individual Entrepreneur
    val entrepreneurName: String = "",   // Full name as per Aadhaar or group registration

    // II. Type of product and value chain
    val productAndValueChain: String = "", // Main product category & value chain

    // 1. BUSINESS BACKGROUND
    val receivedProfessionalTraining: Boolean = false, // Did you receive professional training from an expert? (Y/N)
    val trainingDetails: String = "",    // Probe: from whom, where, how long ago
    val yearsInBusiness: Double = 0.0,   // Count from first production/sale, not from training
    val certificateAvailable: Boolean = false, // Certificate available? (Y/N)
    val priorWorkExperienceYears: Double = 0.0, // Prior work experience (years)
    val howGotIntoBusiness: String = "Self-explored", // Family tradition, Self-explored, Promoted by someone, Government scheme, SHG / group, Other
    val howGotIntoBusinessDescription: String = "", // Description in own words

    // 2. INCOME & PROFITABILITY
    val timeToBreakEven: String = "Under 12 months", // From first investment to first month with no loss (e.g. "8 months")
    val currentMonthlyProfitability: Double = 0.0, // Current monthly profitability (₹)
    val partnersProfitSharingCount: Int = 0, // Partners (profit-sharing)
    val profitSharePercentPerPartner: Double = 0.0, // Profit share % per partner
    val membersCostToCostBasisCount: Int = 0, // Members on cost-to-cost basis
    val earningsPerCostToCostMemberMonthly: Double = 0.0, // Earnings per cost-to-cost member (₹/month)

    // 3. MARKET EXPOSURE
    val dedicatedMarketingMember: String = "Self-managed", // "Yes — dedicated member", "Self-managed", "No marketing activity"
    val marketLevelsServed: String = "B2C — direct household sales", // Comma-separated: "B2C — direct household sales, B2B — traders / shops, Institutional buyers, Online platforms, Export / outside district, SHG / group sales"
    val largestRevenueShareMarketLevel: String = "B2C — direct household sales", // Largest revenue share level

    // 4. INFRASTRUCTURE & OPERATIONS
    val infrastructureJourneyMilestones: String = "", // Major milestones from start to today
    val isInfrastructureSufficient: String = "Sufficient — no changes planned", // "Sufficient — no changes planned", "Sufficient — but upgrades planned", "Not sufficient"
    val downtimeManagementAndImpact: String = "", // How is downtime managed? Downtime events & impact

    // 5. GROWTH PLAN & INPUT MANAGEMENT
    val expansionAspirations: String = "Yes — market demand driven", // "Yes — market demand driven", "Yes — profitability driven", "No expansion plans"
    val expansionDetails: String = "",    // Expansion details (what and why)
    val batchFrequency: String = "Weekly", // e.g. "Weekly", "Daily", "Monthly"
    val outputPerBatch: String = "",      // e.g. "25 baskets"
    val cyclesPerYear: Int = 12,          // e.g. 48
    val inputSupplySelectionBasis: String = "Cost-based selection", // "Defined regular vendors", "Quality-based selection", "Cost-based selection", "Logistics / proximity", "Ad hoc purchases"

    // 6. BUSINESS OUTLOOK & MINDSET
    val mindsetOrientation: String = "Opportunity-driven", // "Mostly self-centric", "Mix of both", "Opportunity-driven"
    val sharesKnowledgeOpenly: String = "Yes — freely", // "Yes — freely", "Selective sharing", "Rarely shares"

    // 7. BUSINESS MANAGEMENT & RECORDS ("Practised", "Partial", "Not done")
    val recordFinancial: String = "Partial",
    val recordCustomer: String = "Practised",
    val recordVendor: String = "Not done",
    val recordSalesData: String = "Practised",
    val recordStatutoryCompliance: String = "Not done",
    val recordFinanceSeparateHousehold: String = "Partial",
    val recordTransparencyPartners: String = "Practised",
    val recordCreditClean: String = "Practised",
    val recordKeepingNotes: String = "",
    val productQualityMeasuresTaken: String = "",

    // 8. TECHNOLOGY & DIGITAL ENGAGEMENT
    val techPracticesUsed: String = "",  // Comma-separated
    val futureAspiration3Years: String = "",

    // ADMIN & METADATA
    val fieldFellowName: String = "Field fellow / DIC team",
    val screeningDate: String = "2026-04-01",
    val status: String = "COMPLETED",    // "DRAFT", "COMPLETED", "RECOMMENDED_FOR_UDYAMI", "NEEDS_UPGRADING"
    val readinessScore: Int = 0,         // 0 to 100 calculated enterprise readiness score
    val createdTimestamp: Long = System.currentTimeMillis()
)
