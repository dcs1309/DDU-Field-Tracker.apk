package com.example.data.analytics

import com.example.data.analytics.model.OpportunityScoreResult

/**
 * Weights configuration for calculating DDU Opportunity Score.
 * Fully configurable by administrators.
 */
data class ScoringWeights(
    val demandVolumeWeight: Double = 15.0,
    val institutionalRepeatDemandWeight: Double = 15.0,
    val numberOfBuyersWeight: Double = 10.0,
    val procurementDistanceWeight: Double = 10.0, // High distance outside block = high localisation opportunity
    val localProductionCapabilityWeight: Double = 15.0,
    val swsmInterestWeight: Double = 10.0,
    val estimatedMarginWeight: Double = 10.0,
    val dataConfidenceWeight: Double = 15.0
)

object OpportunityScoringService {

    var currentWeights = ScoringWeights()

    /**
     * Calculates the DDU Opportunity Score (0 to 100) and categorizes it.
     */
    fun calculateScore(
        demandVolumeMonthly: Double,
        institutionCount: Int,
        retailerCount: Int,
        isProcuredOutsideBlock: Boolean,
        hasCapableSwsmGroup: Boolean,
        hasCapableVaibhavi: Boolean,
        marginPercentEstimate: Double,
        dataConfidenceLevel: String, // HIGH, MEDIUM, LOW
        hasSamplesRequested: Boolean = false
    ): OpportunityScoreResult {
        val breakdown = mutableMapOf<String, Int>()

        // 1. Demand Volume (0-15)
        val volumeScore = when {
            demandVolumeMonthly >= 500 -> 15
            demandVolumeMonthly >= 200 -> 12
            demandVolumeMonthly >= 50 -> 8
            demandVolumeMonthly > 0 -> 4
            else -> 0
        }
        breakdown["Demand Volume"] = volumeScore

        // 2. Institutional Repeat Demand (0-15)
        val instScore = when {
            institutionCount >= 4 -> 15
            institutionCount >= 2 -> 11
            institutionCount >= 1 -> 7
            else -> 2
        }
        breakdown["Institutional Demand"] = instScore

        // 3. Number of Diverse Buyers (0-10)
        val totalBuyers = institutionCount + retailerCount
        val buyerScore = when {
            totalBuyers >= 6 -> 10
            totalBuyers >= 3 -> 7
            totalBuyers >= 1 -> 4
            else -> 1
        }
        breakdown["Buyer Diversity"] = buyerScore

        // 4. Procurement Distance & Localisation Gap (0-10)
        val distanceScore = if (isProcuredOutsideBlock) 10 else 4
        breakdown["External Sourcing Gap"] = distanceScore

        // 5. Local Production Capability (0-15)
        val productionScore = when {
            hasCapableSwsmGroup && hasCapableVaibhavi -> 15
            hasCapableSwsmGroup || hasCapableVaibhavi -> 12
            else -> 4
        }
        breakdown["Production Capability"] = productionScore

        // 6. Producer Interest & Linkage (0-10)
        val linkageScore = if (hasCapableSwsmGroup || hasSamplesRequested) 10 else 5
        breakdown["SWSM / Producer Interest"] = linkageScore

        // 7. Estimated Margin & Economic Viability (0-10)
        val marginScore = when {
            marginPercentEstimate >= 25.0 -> 10
            marginPercentEstimate >= 15.0 -> 7
            marginPercentEstimate >= 8.0 -> 4
            else -> 2
        }
        breakdown["Estimated Margin"] = marginScore

        // 8. Data Confidence & Verification (0-15)
        val confScore = when (dataConfidenceLevel.uppercase()) {
            "HIGH" -> 15
            "MEDIUM" -> 10
            "LOW" -> 4
            else -> 2
        }
        breakdown["Data Confidence"] = confScore

        val totalScore = (volumeScore + instScore + buyerScore + distanceScore + productionScore + linkageScore + marginScore + confScore).coerceIn(0, 100)

        val category = when {
            totalScore >= 75 -> "High Potential"
            totalScore >= 50 -> "Moderate Potential"
            totalScore >= 28 -> "Needs Validation"
            else -> "Insufficient Data"
        }

        val explanation = buildString {
            append("Opportunity score is evaluated at $totalScore/100 ($category). ")
            if (isProcuredOutsideBlock) {
                append("Buyers currently source externally creating a high localisation arbitrage. ")
            }
            if (hasCapableSwsmGroup) {
                append("Local SWSM group capability matches product specifications. ")
            }
            if (instScore >= 11) {
                append("Multiple institutional repeat buyers confirmed in block. ")
            }
        }

        val recommendation = when (category) {
            "High Potential" -> "Immediate Stage 2 sample testing and formal procurement commitment recommended."
            "Moderate Potential" -> "Conduct SWSM readiness validation and supplier pricing audit before pilot."
            "Needs Validation" -> "Collect at least 2 additional buyer surveys and verify external wholesale price."
            else -> "Insufficient field evidence. Conduct structured institutional and retail survey."
        }

        return OpportunityScoreResult(
            score = totalScore,
            category = category,
            breakdownFactors = breakdown,
            explanationText = explanation,
            recommendationText = recommendation
        )
    }
}
