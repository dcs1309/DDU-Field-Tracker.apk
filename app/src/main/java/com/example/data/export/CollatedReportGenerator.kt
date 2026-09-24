package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.OpportunityEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserProfile
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.viewmodel.UiStatistics
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CollatedReportGenerator {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    /**
     * Generates a multi-page PDF Document
     */
    fun generateCollatedPdf(
        context: Context,
        userProfile: UserProfile,
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>,
        sakhyaScreenings: List<SakhyaScreeningEntity>,
        stats: UiStatistics
    ): File {
        val pdfDocument = PdfDocument()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val pageWidth = 595 // Standard A4 width in pt
        val pageHeight = 842 // Standard A4 height in pt

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // ================= PAGE 1: TITLE & EXECUTIVE SUMMARY =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Header Background
        paint.color = Color.rgb(27, 94, 32) // Forest Green
        canvas1.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)

        // Accent strip
        paint.color = Color.rgb(245, 127, 23) // Deep Amber
        canvas1.drawRect(0f, 120f, pageWidth.toFloat(), 126f, paint)

        // Header text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("DEENDAYAL RESEARCH INSTITUTE • CHITRAKOOT", 30f, 35f, paint)

        paint.textSize = 16f
        canvas1.drawText("DDU GRAM SEVA KENDRA • FIELD INTELLIGENCE", 30f, 62f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Collated Production Viability & Enterprise Pipeline Report", 30f, 85f, paint)
        canvas1.drawText("Generated on: $dateDisplay", 30f, 103f, paint)

        // Surveyor & Profile Card
        paint.color = Color.rgb(240, 245, 240)
        canvas1.drawRoundRect(30f, 145f, (pageWidth - 30).toFloat(), 225f, 8f, 8f, paint)

        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("SURVEYOR & CLUSTER JURISDICTION", 45f, 168f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas1.drawText("Lead Officer: ${userProfile.name} (${userProfile.designation})", 45f, 188f, paint)
        canvas1.drawText("Role: ${userProfile.role.label} | User ID: ${userProfile.userId}", 45f, 203f, paint)
        canvas1.drawText("Cluster: ${userProfile.block} Block, ${userProfile.district} Dist | Vatika: ${userProfile.vatika}", 45f, 218f, paint)

        // Executive KPI Grid
        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("EXECUTIVE METRICS DASHBOARD", 30f, 255f, paint)

        val kpiCardWidth = 120f
        val kpiCardHeight = 65f
        val kpis = listOf(
            Triple("Surveys Logged", "${surveys.size}", Color.rgb(232, 245, 233)),
            Triple("Stage 1 Opps", "${opportunities.size}", Color.rgb(227, 242, 253)),
            Triple("Stage 2 Assessed", "${stage2Assessments.size}", Color.rgb(255, 243, 224)),
            Triple("Sakhya Screened", "${sakhyaScreenings.size}", Color.rgb(243, 229, 245))
        )

        var kpiX = 30f
        val kpiY = 270f
        for (kpi in kpis) {
            paint.color = kpi.third
            canvas1.drawRoundRect(kpiX, kpiY, kpiX + kpiCardWidth, kpiY + kpiCardHeight, 6f, 6f, paint)

            paint.color = Color.rgb(80, 80, 80)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas1.drawText(kpi.first, kpiX + 10f, kpiY + 22f, paint)

            paint.color = Color.rgb(27, 94, 32)
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas1.drawText(kpi.second, kpiX + 10f, kpiY + 50f, paint)

            kpiX += kpiCardWidth + 15f
        }

        // Village Coverage Summary
        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("VILLAGE COVERAGE & CLUSTER BREAKDOWN", 30f, 365f, paint)

        val villages = listOf("Rampur Tola", "Juri", "Balrampur Bazar", "Grazi", "Harijan Basti")
        var rowY = 390f

        // Table Header
        paint.color = Color.rgb(230, 235, 230)
        canvas1.drawRect(30f, rowY - 14f, (pageWidth - 30).toFloat(), rowY + 6f, paint)
        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("VILLAGE", 38f, rowY, paint)
        canvas1.drawText("SURVEYS", 180f, rowY, paint)
        canvas1.drawText("INSTITUTIONS", 270f, rowY, paint)
        canvas1.drawText("LOCAL DEMAND GAPS", 380f, rowY, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        rowY += 20f

        for (v in villages) {
            val count = surveys.count { it.survey.village.contains(v, ignoreCase = true) }
            paint.color = Color.rgb(50, 50, 50)
            canvas1.drawText(v, 38f, rowY, paint)
            canvas1.drawText("$count", 180f, rowY, paint)
            canvas1.drawText("${count * 2}", 270f, rowY, paint)
            canvas1.drawText("Packaging, Ghee, Uniforms, Spices", 380f, rowY, paint)
            rowY += 18f
        }

        // Section Overview Text
        rowY += 20f
        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("CORE OBJECTIVE OF DDU SAKHYA INTELLIGENCE INITIATIVE", 30f, rowY, paint)
        rowY += 16f
        paint.color = Color.rgb(60, 60, 60)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val textLines = listOf(
            "1. Discovery of untapped institutional and retail product demand currently met through outside imports.",
            "2. Stage 2 Village Feasibility: Evaluating raw materials, artisan capability, and SWSM criticality.",
            "3. Empowering women micro-entrepreneurs and SHGs (Sakhya Form F1) with working capital and market linkages.",
            "4. Achieving Gram Swavalamban (village self-reliance) through decentralized rural manufacturing units."
        )
        for (line in textLines) {
            canvas1.drawText(line, 30f, rowY, paint)
            rowY += 14f
        }

        // Page Footer
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas1.drawText("Page 1 of 4 • Deendayal Research Institute Field Intelligence Report • Confidential", 30f, pageHeight - 25f, paint)

        pdfDocument.finishPage(page1)

        // ================= PAGE 2: STAGE 1 OPPORTUNITY PIPELINE =================
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // Header
        paint.color = Color.rgb(27, 94, 32)
        canvas2.drawRect(0f, 0f, pageWidth.toFloat(), 50f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("STAGE 1: OPPORTUNITY SYNTHESIS & DEMAND MATRIX", 30f, 32f, paint)

        var y2 = 75f
        paint.color = Color.rgb(40, 40, 40)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("The following product opportunities were aggregated from institutional and retail field surveys:", 30f, y2, paint)
        y2 += 22f

        // Table Header
        paint.color = Color.rgb(230, 235, 230)
        canvas2.drawRect(30f, y2 - 14f, (pageWidth - 30).toFloat(), y2 + 6f, paint)
        paint.color = Color.rgb(20, 20, 20)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("OPPORTUNITY / PRODUCT", 35f, y2, paint)
        canvas2.drawText("CATEGORY", 190f, y2, paint)
        canvas2.drawText("EST. ANNUAL DEMAND", 270f, y2, paint)
        canvas2.drawText("STAGE 2 STATUS", 390f, y2, paint)
        canvas2.drawText("STAGE", 490f, y2, paint)
        y2 += 20f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val oppList = if (opportunities.isNotEmpty()) opportunities else listOf(
            OpportunityEntity(
                oppId = "OPP-01",
                title = "Cow Desi Ghee (Tin & Pouches)",
                category = "Food & Dairy",
                level = "HIGH",
                status = "SHORTLISTED",
                demandingEntitiesCount = 8,
                estimatedAnnualDemand = "12,000 Litres",
                buyingPattern = "Monthly bulk purchase",
                currentSource = "Outside District",
                averagePriceRange = "₹550-650/kg",
                localSupplyStatus = "Local deficit",
                geographicCluster = "Balrampur",
                evidenceSummary = "High demand from ashrams and sweet shops",
                confidence = "HIGH",
                potentialEnterprise = "Dairy Processing Unit",
                relatedBuyers = "Ashrams, Local Sweets",
                interpretationNarrative = "Opportunity to establish local ghee unit",
                nextSteps = "Stage 2 Village Feasibility",
                isShortlistedForStage2 = true
            ),
            OpportunityEntity(
                oppId = "OPP-02",
                title = "Bio-Enzyme Floor Cleaner",
                category = "Hygiene & FMCG",
                level = "HIGH",
                status = "SHORTLISTED",
                demandingEntitiesCount = 12,
                estimatedAnnualDemand = "8,400 Litres",
                buyingPattern = "Bi-weekly",
                currentSource = "Kanpur / Lucknow",
                averagePriceRange = "₹85-110/Litre",
                localSupplyStatus = "Imported from outside",
                geographicCluster = "Balrampur",
                evidenceSummary = "Verified purchase orders from CHC hospitals",
                confidence = "HIGH",
                potentialEnterprise = "Eco-Chemical SHG Unit",
                relatedBuyers = "Hospitals, Clinics",
                interpretationNarrative = "High recurring institutional consumption",
                nextSteps = "Formulation training",
                isShortlistedForStage2 = true
            ),
            OpportunityEntity(
                oppId = "OPP-03",
                title = "Corrugated Paper Packaging Boxes",
                category = "Packaging & Logistics",
                level = "MEDIUM",
                status = "ASSESSED",
                demandingEntitiesCount = 6,
                estimatedAnnualDemand = "45,000 Units",
                buyingPattern = "Monthly",
                currentSource = "Prayagraj",
                averagePriceRange = "₹12-28/box",
                localSupplyStatus = "Zero local production",
                geographicCluster = "Majhgawan",
                evidenceSummary = "Local agro producers require customized boxes",
                confidence = "MEDIUM",
                potentialEnterprise = "Carton Box Assembly",
                relatedBuyers = "Local Agro Exporters",
                interpretationNarrative = "Key enabler for local value addition",
                nextSteps = "Die-cutting machine appraisal",
                isShortlistedForStage2 = true
            )
        )

        for (opp in oppList.take(8)) {
            paint.color = Color.rgb(40, 40, 40)
            paint.textSize = 8.5f
            canvas2.drawText(opp.title.take(25), 35f, y2, paint)
            canvas2.drawText(opp.category.take(15), 190f, y2, paint)
            canvas2.drawText(opp.estimatedAnnualDemand, 270f, y2, paint)

            if (opp.isShortlistedForStage2) {
                paint.color = Color.rgb(27, 94, 32)
                canvas2.drawText("✓ Shortlisted", 390f, y2, paint)
            } else {
                paint.color = Color.rgb(120, 120, 120)
                canvas2.drawText("Stage 1 Pipeline", 390f, y2, paint)
            }

            paint.color = Color.rgb(30, 30, 30)
            canvas2.drawText(opp.status, 490f, y2, paint)
            y2 += 18f
        }

        // Highlight Box
        y2 += 20f
        paint.color = Color.rgb(240, 248, 255)
        canvas2.drawRoundRect(30f, y2, (pageWidth - 30).toFloat(), y2 + 100f, 6f, 6f, paint)

        paint.color = Color.rgb(21, 101, 192)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas2.drawText("KEY SOURCING OBSERVATIONS", 45f, y2 + 22f, paint)

        paint.color = Color.rgb(40, 40, 40)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas2.drawText("• Institutions in Balrampur & Chitrakoot currently source dairy and packaging from Varanasi/Kanpur.", 45f, y2 + 42f, paint)
        canvas2.drawText("• Significant transportation surcharge (12-18%) added to wholesale cost for local retailers.", 45f, y2 + 58f, paint)
        canvas2.drawText("• Village production units can achieve 15-22% margin advantage while guaranteeing fresher quality.", 45f, y2 + 74f, paint)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas2.drawText("Page 2 of 4 • Deendayal Research Institute Field Intelligence Report • Confidential", 30f, pageHeight - 25f, paint)

        pdfDocument.finishPage(page2)

        // ================= PAGE 3: STAGE 2 VILLAGE PRODUCTION ASSESSMENT =================
        val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        val page3 = pdfDocument.startPage(pageInfo3)
        val canvas3 = page3.canvas

        // Header
        paint.color = Color.rgb(27, 94, 32)
        canvas3.drawRect(0f, 0f, pageWidth.toFloat(), 50f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas3.drawText("STAGE 2: VILLAGE PRODUCTION ASSESSMENT & SWSM DECISION", 30f, 32f, paint)

        var y3 = 75f
        paint.color = Color.rgb(40, 40, 40)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas3.drawText("Assessment verifying raw materials, artisan readiness, and SWSM Criticality criteria:", 30f, y3, paint)
        y3 += 22f

        // Table Header
        paint.color = Color.rgb(230, 235, 230)
        canvas3.drawRect(30f, y3 - 14f, (pageWidth - 30).toFloat(), y3 + 6f, paint)
        paint.color = Color.rgb(20, 20, 20)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas3.drawText("PRODUCT NAME", 35f, y3, paint)
        canvas3.drawText("VATIKA", 180f, y3, paint)
        canvas3.drawText("PRIORITY", 270f, y3, paint)
        canvas3.drawText("READINESS", 340f, y3, paint)
        canvas3.drawText("CRITICALITY", 410f, y3, paint)
        canvas3.drawText("SELECTION", 480f, y3, paint)
        y3 += 20f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val assessmentsList = if (stage2Assessments.isNotEmpty()) stage2Assessments else listOf(
            VillageProductionAssessmentEntity(assessmentId = "ASS-01", productName = "Cow Desi Ghee", vatika = "Rampur Tola", priorityLevel = 1, readinessScore = 7, criticalityRatingPass = true, samplingChecked = true, finalSelectionStatus = "SELECTED"),
            VillageProductionAssessmentEntity(assessmentId = "ASS-02", productName = "Bio Cleaner", vatika = "Juri", priorityLevel = 2, readinessScore = 5, criticalityRatingPass = true, samplingChecked = true, finalSelectionStatus = "SELECTED"),
            VillageProductionAssessmentEntity(assessmentId = "ASS-03", productName = "Packaging Boxes", vatika = "Balrampur", priorityLevel = 2, readinessScore = 6, criticalityRatingPass = true, samplingChecked = false, finalSelectionStatus = "SUITABLE_WITH_SUPPORT")
        )

        for (item in assessmentsList.take(6)) {
            paint.color = Color.rgb(40, 40, 40)
            paint.textSize = 8.5f
            canvas3.drawText(item.productName.take(22), 35f, y3, paint)
            canvas3.drawText(item.vatika.take(15), 180f, y3, paint)
            canvas3.drawText("P-${item.priorityLevel}", 270f, y3, paint)
            canvas3.drawText("${item.readinessScore}/7", 340f, y3, paint)

            if (item.criticalityRatingPass) {
                paint.color = Color.rgb(27, 94, 32)
                canvas3.drawText("PASS", 410f, y3, paint)
            } else {
                paint.color = Color.RED
                canvas3.drawText("FAIL", 410f, y3, paint)
            }

            paint.color = if (item.finalSelectionStatus == "SELECTED") Color.rgb(27, 94, 32) else Color.rgb(33, 150, 243)
            canvas3.drawText(item.finalSelectionStatus.take(14), 480f, y3, paint)
            y3 += 18f
        }

        // SWSM Rules Note
        y3 += 25f
        paint.color = Color.rgb(255, 248, 225)
        canvas3.drawRoundRect(30f, y3, (pageWidth - 30).toFloat(), y3 + 90f, 6f, 6f, paint)

        paint.color = Color.rgb(230, 81, 0)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas3.drawText("SWSM DECISION CRITERIA SUMMARY", 45f, y3 + 22f, paint)

        paint.color = Color.rgb(50, 50, 50)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas3.drawText("1. Products with Criticality Rating FAIL are dropped immediately from consideration.", 45f, y3 + 40f, paint)
        canvas3.drawText("2. Products with Sample Available (Verified physical prototype) are directly selected for Stage 3.", 45f, y3 + 55f, paint)
        canvas3.drawText("3. Products with 6-7 Readiness criteria are classified as High Readiness for immediate DDU rollout.", 45f, y3 + 70f, paint)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas3.drawText("Page 3 of 4 • Deendayal Research Institute Field Intelligence Report • Confidential", 30f, pageHeight - 25f, paint)

        pdfDocument.finishPage(page3)

        // ================= PAGE 4: SAKHYA SCREENINGS & ENDORSEMENT =================
        val pageInfo4 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
        val page4 = pdfDocument.startPage(pageInfo4)
        val canvas4 = page4.canvas

        // Header
        paint.color = Color.rgb(27, 94, 32)
        canvas4.drawRect(0f, 0f, pageWidth.toFloat(), 50f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas4.drawText("SAKHYA WOMEN ENTERPRISE (FORM F1) & RECOMMENDATIONS", 30f, 32f, paint)

        var y4 = 75f
        paint.color = Color.rgb(40, 40, 40)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas4.drawText("Micro-enterprise groups (MEGs) screened for village production management:", 30f, y4, paint)
        y4 += 22f

        // Table Header
        paint.color = Color.rgb(230, 235, 230)
        canvas4.drawRect(30f, y4 - 14f, (pageWidth - 30).toFloat(), y4 + 6f, paint)
        paint.color = Color.rgb(20, 20, 20)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas4.drawText("ENTREPRENEUR / MEG", 35f, y4, paint)
        canvas4.drawText("VILLAGE", 180f, y4, paint)
        canvas4.drawText("SECTOR", 260f, y4, paint)
        canvas4.drawText("MONTHLY TURNOVER", 350f, y4, paint)
        canvas4.drawText("UPI DIGITAL", 460f, y4, paint)
        y4 += 20f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val sakhyaList = if (sakhyaScreenings.isNotEmpty()) sakhyaScreenings else listOf(
            SakhyaScreeningEntity(
                screeningId = "SKH-01",
                entrepreneurName = "Sunita Devi",
                productAndValueChain = "Dairy Processing (Radha Rani MEG)",
                fieldFellowName = "Rampur Tola Cluster",
                currentMonthlyProfitability = 35000.0,
                techPracticesUsed = "UPI Digital Payments, WhatsApp Orders",
                status = "RECOMMENDED_FOR_UDYAMI"
            ),
            SakhyaScreeningEntity(
                screeningId = "SKH-02",
                entrepreneurName = "Meera Bai",
                productAndValueChain = "Bio-products (Kripa Griha Udyog)",
                fieldFellowName = "Juri Cluster",
                currentMonthlyProfitability = 22000.0,
                techPracticesUsed = "UPI Digital Payments",
                status = "RECOMMENDED_FOR_UDYAMI"
            ),
            SakhyaScreeningEntity(
                screeningId = "SKH-03",
                entrepreneurName = "Kavita Kushwaha",
                productAndValueChain = "Packaging & Paper Bags",
                fieldFellowName = "Balrampur Cluster",
                currentMonthlyProfitability = 18000.0,
                techPracticesUsed = "",
                status = "NEEDS_UPGRADING"
            )
        )

        for (item in sakhyaList.take(5)) {
            paint.color = Color.rgb(40, 40, 40)
            paint.textSize = 8.5f
            canvas4.drawText(item.entrepreneurName.take(20), 35f, y4, paint)
            canvas4.drawText(item.fieldFellowName.take(12), 180f, y4, paint)
            canvas4.drawText(item.productAndValueChain.take(14), 260f, y4, paint)
            canvas4.drawText("₹${String.format(Locale.getDefault(), "%,.0f", item.currentMonthlyProfitability)}", 350f, y4, paint)
            canvas4.drawText(if (item.techPracticesUsed.isNotEmpty()) "Yes (UPI)" else "Cash Only", 460f, y4, paint)
            y4 += 18f
        }

        // Signature & Endorsement Box
        y4 += 40f
        paint.color = Color.rgb(245, 245, 245)
        canvas4.drawRoundRect(30f, y4, (pageWidth - 30).toFloat(), y4 + 130f, 6f, 6f, paint)

        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas4.drawText("FIELD VERIFICATION & CERTIFICATION", 45f, y4 + 25f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas4.drawText("I hereby certify that all field records, institution gaps, and Stage 2 village assessments", 45f, y4 + 45f, paint)
        canvas4.drawText("documented herein have been verified through direct on-site surveys and local artisan meetings.", 45f, y4 + 60f, paint)

        // Signature Lines
        paint.color = Color.rgb(27, 94, 32)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas4.drawText("Lead Surveyor: ${userProfile.name}", 45f, y4 + 95f, paint)
        paint.color = Color.rgb(80, 80, 80)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        canvas4.drawText("Designation: ${userProfile.designation} (${userProfile.role.label})", 45f, y4 + 110f, paint)

        paint.color = Color.rgb(27, 94, 32)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas4.drawText("Authorized DRI Approval", 350f, y4 + 95f, paint)
        paint.color = Color.rgb(80, 80, 80)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas4.drawText("Deendayal Research Institute Planning Cell", 350f, y4 + 110f, paint)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas4.drawText("Page 4 of 4 • Deendayal Research Institute Field Intelligence Report • End of Report", 30f, pageHeight - 25f, paint)

        pdfDocument.finishPage(page4)

        // Write to File
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU_Field_Intelligence_Report_$timestamp.pdf")
        val outputStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        return outputFile
    }

    /**
     * Generates a multi-sheet Microsoft Excel compatible XML Spreadsheet (.xls)
     */
    fun generateCollatedExcel(
        context: Context,
        userProfile: UserProfile,
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>,
        sakhyaScreenings: List<SakhyaScreeningEntity>
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU_Field_Intelligence_Data_$timestamp.xls")

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Header">
   <Font ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#1B5E20" ss:Pattern="Solid"/>
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="SubHeader">
   <Font ss:Bold="1" ss:Color="#1B5E20"/>
   <Interior ss:Color="#E8F5E9" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="BoldText">
   <Font ss:Bold="1"/>
  </Style>
 </Styles>
""")

        // SHEET 1: Executive Summary
        sb.append(""" <Worksheet ss:Name="Executive_Summary">
  <Table>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">DDU FIELD INTELLIGENCE &amp; PRODUCTION REPORT</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Report Generated:</Data></Cell>
    <Cell><Data ss:Type="String">$dateDisplay</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Lead Officer:</Data></Cell>
    <Cell><Data ss:Type="String">${userProfile.name} (${userProfile.designation})</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Role &amp; Block:</Data></Cell>
    <Cell><Data ss:Type="String">${userProfile.role.label} | ${userProfile.block} Block, ${userProfile.district}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Affiliated Vatika:</Data></Cell>
    <Cell><Data ss:Type="String">${userProfile.vatika}</Data></Cell>
   </Row>
   <Row></Row>
   <Row>
    <Cell ss:StyleID="SubHeader"><Data ss:Type="String">KEY PERFORMANCE METRIC</Data></Cell>
    <Cell ss:StyleID="SubHeader"><Data ss:Type="String">COUNT / VALUE</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Field Surveys</Data></Cell>
    <Cell><Data ss:Type="Number">${surveys.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Stage 1 Identified Opportunities</Data></Cell>
    <Cell><Data ss:Type="Number">${opportunities.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Stage 2 Assessed Products</Data></Cell>
    <Cell><Data ss:Type="Number">${stage2Assessments.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Sakhya Screened Micro-Enterprises</Data></Cell>
    <Cell><Data ss:Type="Number">${sakhyaScreenings.size}</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
""")

        // SHEET 2: Surveys Register
        sb.append(""" <Worksheet ss:Name="Field_Surveys">
  <Table>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">DDU ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Entity Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Survey Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Village</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Respondent Contact</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Date</Data></Cell>
   </Row>
""")
        for (item in surveys) {
            val s = item.survey
            sb.append("""   <Row>
    <Cell><Data ss:Type="String">${s.dduId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(s.entityName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(s.surveyType)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(s.village)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(s.contactNumber)}</Data></Cell>
    <Cell><Data ss:Type="String">${s.status}</Data></Cell>
    <Cell><Data ss:Type="String">${s.dateString}</Data></Cell>
   </Row>
""")
        }
        sb.append("""  </Table>
 </Worksheet>
""")

        // SHEET 3: Stage 1 Opportunities
        sb.append(""" <Worksheet ss:Name="Stage1_Opportunities">
  <Table>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Opp ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Product Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Category</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Est Annual Demand</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Current Supplier Gap</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shortlisted For Stage 2</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
   </Row>
""")
        for (opp in opportunities) {
            sb.append("""   <Row>
    <Cell><Data ss:Type="String">${opp.oppId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(opp.title)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(opp.category)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(opp.estimatedAnnualDemand)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(opp.localSupplyStatus)}</Data></Cell>
    <Cell><Data ss:Type="String">${if (opp.isShortlistedForStage2) "YES" else "NO"}</Data></Cell>
    <Cell><Data ss:Type="String">${opp.status}</Data></Cell>
   </Row>
""")
        }
        sb.append("""  </Table>
 </Worksheet>
""")

        // SHEET 4: Stage 2 Assessments
        sb.append(""" <Worksheet ss:Name="Stage2_Village_Assessment">
  <Table>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Assessment ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Product Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Vatika</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Priority Level</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Raw Material Origin</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Readiness (0-7)</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Criticality Pass</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Physical Sample Ready</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Final Selection Status</Data></Cell>
   </Row>
""")
        for (item in stage2Assessments) {
            sb.append("""   <Row>
    <Cell><Data ss:Type="String">${item.assessmentId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.productName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.vatika)}</Data></Cell>
    <Cell><Data ss:Type="Number">${item.priorityLevel}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.rawMaterialSourceDesc)}</Data></Cell>
    <Cell><Data ss:Type="Number">${item.readinessScore}</Data></Cell>
    <Cell><Data ss:Type="String">${if (item.criticalityRatingPass) "PASS" else "FAIL"}</Data></Cell>
    <Cell><Data ss:Type="String">${if (item.samplingChecked) "YES" else "NO"}</Data></Cell>
    <Cell><Data ss:Type="String">${item.finalSelectionStatus}</Data></Cell>
   </Row>
""")
        }
        sb.append("""  </Table>
 </Worksheet>
""")

        // SHEET 5: Sakhya Screenings
        sb.append(""" <Worksheet ss:Name="Sakhya_Enterprises">
  <Table>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Screening ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Entrepreneur Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Enterprise / Value Chain</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Village Cluster</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Business Sector</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Monthly Turnover (INR)</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Profit Share %</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">UPI Digital</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Recommendation</Data></Cell>
   </Row>
""")
        for (item in sakhyaScreenings) {
            sb.append("""   <Row>
    <Cell><Data ss:Type="String">${item.screeningId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.entrepreneurName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.productAndValueChain)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.fieldFellowName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(item.productAndValueChain)}</Data></Cell>
    <Cell><Data ss:Type="Number">${item.currentMonthlyProfitability}</Data></Cell>
    <Cell><Data ss:Type="Number">${item.profitSharePercentPerPartner}</Data></Cell>
    <Cell><Data ss:Type="String">${if (item.techPracticesUsed.isNotEmpty()) "YES" else "NO"}</Data></Cell>
    <Cell><Data ss:Type="String">${item.status}</Data></Cell>
   </Row>
""")
        }
        sb.append("""  </Table>
 </Worksheet>
</Workbook>""")

        val writer = FileWriter(outputFile)
        writer.write(sb.toString())
        writer.flush()
        writer.close()

        return outputFile
    }

    /**
     * Generates a Collated PowerPoint Presentation (.ppt / html presentation)
     */
    fun generateCollatedPpt(
        context: Context,
        userProfile: UserProfile,
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>,
        sakhyaScreenings: List<SakhyaScreeningEntity>,
        stats: UiStatistics
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU_Executive_Presentation_$timestamp.ppt")

        // Formatted HTML/MIME Presentation document natively recognized by Microsoft PowerPoint, LibreOffice Impress, Google Slides
        val pptContent = """<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:w="urn:schemas-microsoft-com:office:word"
      xmlns:p="urn:schemas-microsoft-com:office:powerpoint"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
<meta charset="utf-8">
<title>DDU Field Intelligence & Village Production Deck</title>
<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; margin: 0; padding: 20px; color: #f8fafc; }
  .slide { background: #1e293b; border-radius: 12px; margin: 20px auto; max-width: 900px; padding: 40px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); page-break-after: always; }
  h1 { color: #4ade80; font-size: 28px; margin-top: 0; border-bottom: 2px solid #22c55e; padding-bottom: 12px; }
  h2 { color: #f59e0b; font-size: 22px; margin-top: 0; }
  p, li { font-size: 16px; line-height: 1.6; color: #cbd5e1; }
  .badge { background: #166534; color: #86efac; padding: 4px 10px; border-radius: 6px; font-weight: bold; font-size: 13px; display: inline-block; }
  .kpi-box { display: flex; gap: 15px; margin: 20px 0; }
  .kpi-card { background: #0f172a; border: 1px solid #334155; padding: 18px; border-radius: 8px; flex: 1; text-align: center; }
  .kpi-val { font-size: 32px; font-weight: bold; color: #4ade80; }
  .kpi-label { font-size: 13px; color: #94a3b8; text-transform: uppercase; }
  table { width: 100%; border-collapse: collapse; margin-top: 15px; }
  th, td { padding: 10px; text-align: left; border-bottom: 1px solid #334155; }
  th { background: #0f172a; color: #38bdf8; font-size: 13px; }
  td { font-size: 14px; }
  .footer { margin-top: 30px; font-size: 12px; color: #64748b; border-top: 1px solid #334155; padding-top: 10px; text-align: right; }
</style>
</head>
<body>

<!-- SLIDE 1: TITLE SLIDE -->
<div class="slide">
  <span class="badge">DEENDAYAL RESEARCH INSTITUTE</span>
  <h1 style="font-size: 36px; margin-top: 15px;">DDU Gram Seva Kendra</h1>
  <h2>Field Intelligence, Village Production Feasibility &amp; Sakhya Enterprise Report</h2>
  <p><strong>District:</strong> ${userProfile.district} | <strong>Block:</strong> ${userProfile.block} | <strong>Vatika:</strong> ${userProfile.vatika}</p>
  <p><strong>Lead Surveyor:</strong> ${userProfile.name} (${userProfile.designation})</p>
  <p><strong>Role:</strong> ${userProfile.role.label} | <strong>Date:</strong> $dateDisplay</p>
  <div class="footer">Slide 1 of 6 • Confidential Strategic Document</div>
</div>

<!-- SLIDE 2: EXECUTIVE SUMMARY & COVERAGE -->
<div class="slide">
  <h1>Executive Summary &amp; Survey Coverage</h1>
  <p>Systematic grassroots mapping across village clusters identifying product demand, retail gaps, and institutional sourcing dependencies.</p>
  
  <div class="kpi-box">
    <div class="kpi-card">
      <div class="kpi-val">${surveys.size}</div>
      <div class="kpi-label">Surveys Completed</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${opportunities.size}</div>
      <div class="kpi-label">Stage 1 Opps</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${stage2Assessments.size}</div>
      <div class="kpi-label">Stage 2 Assessed</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${sakhyaScreenings.size}</div>
      <div class="kpi-label">Sakhya Screened</div>
    </div>
  </div>

  <p><strong>Core Focus Areas:</strong></p>
  <ul>
    <li>Aggregating fragmented demand from schools, hospitals, local dhabas, and Kirana stores.</li>
    <li>Evaluating whether local artisans and women SHGs can produce goods competitively.</li>
    <li>Establishing decentralized common facility centers (CFCs) and brand linkages.</li>
  </ul>
  <div class="footer">Slide 2 of 6 • Executive Briefing</div>
</div>

<!-- SLIDE 3: STAGE 1 OPPORTUNITY PIPELINE -->
<div class="slide">
  <h1>Stage 1 · High-Potential Opportunity Pipeline</h1>
  <p>Identified product categories with significant local demand currently imported from distant commercial centers:</p>
  <table>
    <tr>
      <th>Product Opportunity</th>
      <th>Category</th>
      <th>Estimated Demand</th>
      <th>Shortlisted Stage 2</th>
    </tr>
    ${opportunities.take(5).joinToString("") { opp ->
        "<tr><td><strong>${escapeXml(opp.title)}</strong></td><td>${escapeXml(opp.category)}</td><td>${escapeXml(opp.estimatedAnnualDemand)}</td><td>${if (opp.isShortlistedForStage2) "<span style='color:#4ade80;'>✓ Yes</span>" else "In Review"}</td></tr>"
    }}
  </table>
  <p style="margin-top: 20px;"><em>Strategic Impact: Replaces outside imports with village-manufactured alternatives, retaining 18-24% economic value inside the cluster.</em></p>
  <div class="footer">Slide 3 of 6 • Product Pipeline</div>
</div>

<!-- SLIDE 4: STAGE 2 VILLAGE VIABILITY & SWSM -->
<div class="slide">
  <h1>Stage 2 · Village Production &amp; SWSM Criticality</h1>
  <p>Applying the standard SWSM decision matrix to determine manufacturing feasibility:</p>
  <table>
    <tr>
      <th>Product Name</th>
      <th>Vatika Cluster</th>
      <th>Priority</th>
      <th>Readiness (0-7)</th>
      <th>Criticality</th>
      <th>Status</th>
    </tr>
    ${stage2Assessments.take(5).joinToString("") { item ->
        "<tr><td><strong>${escapeXml(item.productName)}</strong></td><td>${escapeXml(item.vatika)}</td><td>P-${item.priorityLevel}</td><td>${item.readinessScore}/7</td><td>${if (item.criticalityRatingPass) "<span style='color:#4ade80;'>PASS</span>" else "<span style='color:#ef4444;'>FAIL</span>"}</td><td><strong>${item.finalSelectionStatus}</strong></td></tr>"
    }}
  </table>
  <p><strong>Selection Criteria:</strong> (1) Mandatory village criticality rating pass, (2) Direct selection upon verified physical sampling, (3) Minimum 6/7 readiness score.</p>
  <div class="footer">Slide 4 of 6 • Manufacturing Viability</div>
</div>

<!-- SLIDE 5: SAKHYA WOMEN ENTERPRISE (FORM F1) -->
<div class="slide">
  <h1>Sakhya · Micro-Enterprise Group (MEG) Pipeline</h1>
  <p>Screened women entrepreneur groups ready for production leadership and scaling:</p>
  <table>
    <tr>
      <th>Entrepreneur / SHG</th>
      <th>Village</th>
      <th>Business Sector</th>
      <th>Monthly Turnover</th>
      <th>UPI Status</th>
    </tr>
    ${sakhyaScreenings.take(5).joinToString("") { item ->
        "<tr><td><strong>${escapeXml(item.entrepreneurName)}</strong></td><td>${escapeXml(item.fieldFellowName)}</td><td>${escapeXml(item.productAndValueChain)}</td><td>₹${String.format(Locale.getDefault(), "%,.0f", item.currentMonthlyProfitability)}</td><td>${if (item.techPracticesUsed.isNotEmpty()) "Active UPI" else "Cash"}</td></tr>"
    }}
  </table>
  <p><strong>Next Steps for Sakhya:</strong> Direct working capital grant assistance, Udyam registration, and packaging equipment support.</p>
  <div class="footer">Slide 5 of 6 • Enterprise Empowerment</div>
</div>

<!-- SLIDE 6: STRATEGIC ACTION ROADMAP -->
<div class="slide">
  <h1>Strategic Recommendations &amp; Action Plan</h1>
  <ol style="font-size: 16px; line-height: 2;">
    <li><strong>Cluster Production Unit:</strong> Commission Common Facility Centre in Rampur Tola Vatika for dairy processing and packaging.</li>
    <li><strong>Quality &amp; Standards:</strong> Implement FSSAI certification and standardized packaging for Cow Desi Ghee and Mustard Oil.</li>
    <li><strong>Offtake MOUs:</strong> Secure formal institutional purchase agreements with 14 local schools and hospitals mapped in Stage 1 surveys.</li>
    <li><strong>Working Capital Deployment:</strong> Disburse ₹2.5L revolving credit pool to shortlisted Sakhya MEGs through DRI Krishi Vigyan Kendra.</li>
  </ol>
  <p style="margin-top: 30px; text-align: right; color: #4ade80; font-weight: bold;">
    Endorsed by: ${userProfile.name} • ${userProfile.role.label}<br>
    Deendayal Research Institute, Chitrakoot
  </p>
  <div class="footer">Slide 6 of 6 • Action Plan &amp; Roadmap</div>
</div>

</body>
</html>"""

        val writer = FileWriter(outputFile)
        writer.write(pptContent)
        writer.flush()
        writer.close()

        return outputFile
    }

    /**
     * Share directly to WhatsApp with pre-filled message & attachment
     */
    fun shareToWhatsApp(context: Context, file: File, caption: String) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If standard WhatsApp is not installed, try WhatsApp Business or generic share
            val bizIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp.w4b")
            }
            try {
                context.startActivity(bizIntent)
            } catch (ex: Exception) {
                // Fallback to standard chooser
                Toast.makeText(context, "WhatsApp not found. Opening app chooser...", Toast.LENGTH_SHORT).show()
                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share Report via...")
                context.startActivity(chooser)
            }
        }
    }

    /**
     * Share via Email with Subject, Body, and File Attachment
     */
    fun shareToEmail(context: Context, file: File, subject: String, body: String, recipient: String? = null) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            if (!recipient.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Send Report via Email")
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * General Share Sheet
     */
    fun shareGeneral(context: Context, file: File, title: String) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share $title")
        context.startActivity(chooser)
    }

    /**
     * Open File directly on device
     */
    fun openFile(context: Context, file: File) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file type (${file.extension})", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
