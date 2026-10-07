package com.example.data.analytics

import com.example.data.analytics.model.DduDataQualityMetric
import com.example.data.model.SurveyWithDetails

object DataQualityService {

    /**
     * Evaluates data quality and completeness for all surveys.
     */
    fun evaluateQuality(surveys: List<SurveyWithDetails>): List<DduDataQualityMetric> {
        val seenNames = mutableMapOf<String, Int>()
        surveys.forEach { item ->
            val norm = item.survey.entityName.trim().lowercase()
            seenNames[norm] = (seenNames[norm] ?: 0) + 1
        }

        return surveys.map { item ->
            val s = item.survey
            val issues = mutableListOf<String>()

            // 1. GPS Check
            val hasGps = (s.gpsLatitude != 0.0 && s.gpsLongitude != 0.0 && s.gpsConfirmed)
            if (!hasGps) issues.add("GPS Pending / Accuracy > 10m")

            // 2. Photos Check
            val hasPhotos = item.evidenceList.any { it.type == "PHOTO" }
            if (!hasPhotos) issues.add("Missing field photograph / shopfront")

            // 3. Contact Details
            val hasContacts = s.contactPerson.isNotBlank() && s.contactNumber.isNotBlank() && s.contactNumber.length >= 10
            if (!hasContacts) issues.add("Missing valid contact number or name")

            // 4. Products & Pricing
            val hasProducts = item.products.isNotEmpty()
            val hasDemandPricing = hasProducts && item.products.all { (it.buyingPrice > 0 || it.sellingPrice > 0) && it.minQuantity > 0 }
            if (!hasProducts) {
                issues.add("No product demand recorded")
            } else if (!hasDemandPricing) {
                issues.add("Missing product price or procurement quantity")
            }

            // 5. Supplier information
            val hasSupplierInfo = item.products.any { it.currentSupplier.isNotBlank() }
            if (!hasSupplierInfo) issues.add("Missing existing supplier / procurement origin")

            // 6. Supervisor Validation
            val isSupervisorVerified = s.status == "APPROVED"
            if (!isSupervisorVerified) issues.add("Pending supervisor validation")

            // 7. Duplicate Check
            val normName = s.entityName.trim().lowercase()
            val isDuplicate = (seenNames[normName] ?: 0) > 1
            if (isDuplicate) issues.add("Potential duplicate business / institution in village")

            // Calculate precise completeness score (0-100)
            var score = 0
            if (hasGps) score += 20
            if (hasPhotos) score += 20
            if (hasContacts) score += 15
            if (hasProducts && hasDemandPricing) score += 20
            if (hasSupplierInfo) score += 15
            if (isSupervisorVerified) score += 10
            score = score.coerceIn(0, 100)

            val statusText = when {
                score >= 90 && isSupervisorVerified -> "Verified & Complete"
                score >= 75 -> "Sufficient Field Data"
                score >= 50 -> "Needs Validation"
                else -> "Incomplete Survey"
            }

            DduDataQualityMetric(
                dduId = s.dduId,
                entityName = s.entityName,
                village = "${s.village}, ${s.block}",
                completenessScore = score,
                hasGps = hasGps,
                hasPhotos = hasPhotos,
                hasContacts = hasContacts,
                hasSupplierInfo = hasSupplierInfo,
                hasDemandPricing = hasDemandPricing,
                isSupervisorVerified = isSupervisorVerified,
                isPotentiallyDuplicate = isDuplicate,
                issuesList = issues,
                statusText = statusText
            )
        }
    }
}
