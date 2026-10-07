package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserProfile
import com.example.data.model.VillageProductionAssessmentEntity
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SingleRecordExportGenerator {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    // =========================================================================
    // 1. DDU 1 FIELD RECORD EXPORTS (PDF / EXCEL / PPTM)
    // =========================================================================

    /**
     * Generates a formal Single Field Record Dossier PDF
     */
    fun generateSurveyPdf(
        context: Context,
        record: SurveyWithDetails,
        userProfile: UserProfile
    ): File {
        val survey = record.survey
        val pdfDocument = PdfDocument()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        val pageWidth = 595
        val pageHeight = 842
        val paint = Paint().apply { isAntiAlias = true }

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Header Background (Forest Green)
        paint.color = Color.rgb(27, 94, 32)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

        // Accent strip (Amber)
        paint.color = Color.rgb(245, 127, 23)
        canvas.drawRect(0f, 110f, pageWidth.toFloat(), 115f, paint)

        // Header text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DEENDAYAL RESEARCH INSTITUTE • CHITRAKOOT", 30f, 32f, paint)

        paint.textSize = 16f
        canvas.drawText("DDU GRAM SEVA KENDRA • FIELD RECORD DOSSIER", 30f, 58f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Official Field Observation Record • Record ID: ${survey.dduId}", 30f, 78f, paint)
        canvas.drawText("Generated: $dateDisplay | Officer: ${userProfile.name} (${userProfile.designation})", 30f, 95f, paint)

        // Entity Overview Card
        var currentY = 135f
        paint.color = Color.rgb(240, 245, 240)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 110f, 8f, 8f, paint)

        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. ENTITY PROFILE & JURISDICTION", 45f, currentY + 24f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Entity Name: ${survey.entityName}", 45f, currentY + 44f, paint)
        canvas.drawText("Entity Type: ${survey.entityType} | Survey Category: ${survey.surveyType}", 45f, currentY + 59f, paint)
        canvas.drawText("Contact Person: ${survey.contactPerson.ifBlank { "N/A" }} | Phone: ${survey.contactNumber.ifBlank { "N/A" }}", 45f, currentY + 74f, paint)
        canvas.drawText("Location: Village ${survey.village}, Tola ${survey.tola.ifBlank { "Main Settlement" }}, Block ${survey.block}, Dist ${survey.district}", 45f, currentY + 89f, paint)
        canvas.drawText("Status: ${survey.status} | Audit Completeness: ${survey.completenessScore}% | Cloud Sync: ${if (survey.isSynced) "Synced" else "Cached Locally"}", 45f, currentY + 103f, paint)

        // GPS Geolocation Card
        currentY += 125f
        paint.color = Color.rgb(243, 244, 246)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 55f, 8f, 8f, paint)

        paint.color = Color.rgb(31, 41, 55)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. GEOGRAPHIC TELEMETRY & GPS FIX", 45f, currentY + 20f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val latStr = String.format(Locale.US, "%.6f° N", survey.gpsLatitude)
        val lngStr = String.format(Locale.US, "%.6f° E", survey.gpsLongitude)
        canvas.drawText("Latitude: $latStr | Longitude: $lngStr | Fix Accuracy: ±${String.format(Locale.US, "%.1f", survey.gpsAccuracyMeters)}m", 45f, currentY + 36f, paint)
        canvas.drawText("GPS Confirmed: ${if (survey.gpsConfirmed) "YES (Ground Verified)" else "Automatic"} | Timestamp: ${survey.dateString} ${survey.timeString}", 45f, currentY + 49f, paint)

        // Demanded Products Table
        currentY += 70f
        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("3. DEMANDED PRODUCTS & COMMERCIAL PROCUREMENT (${record.products.size} Items)", 30f, currentY, paint)

        currentY += 10f
        // Table Header
        paint.color = Color.rgb(230, 235, 230)
        canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 22f, paint)

        paint.color = Color.rgb(30, 30, 30)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUCT NAME", 38f, currentY + 15f, paint)
        canvas.drawText("CATEGORY", 170f, currentY + 15f, paint)
        canvas.drawText("DEMAND / UNIT", 270f, currentY + 15f, paint)
        canvas.drawText("BUYING PRICE", 370f, currentY + 15f, paint)
        canvas.drawText("MONTHLY VALUE", 450f, currentY + 15f, paint)

        currentY += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        var totalDemandValue = 0.0
        if (record.products.isEmpty()) {
            paint.color = Color.rgb(100, 100, 100)
            canvas.drawText("No specific product demand logged for this observation entity.", 40f, currentY + 16f, paint)
            currentY += 26f
        } else {
            record.products.take(8).forEachIndexed { index, prod ->
                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 248)
                    canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 20f, paint)
                }

                paint.color = Color.rgb(33, 33, 33)
                val itemValue = prod.maxQuantity * prod.buyingPrice
                totalDemandValue += itemValue

                canvas.drawText(prod.productName.take(22), 38f, currentY + 14f, paint)
                canvas.drawText(prod.category.take(16), 170f, currentY + 14f, paint)
                canvas.drawText("${prod.maxQuantity.toInt()} ${prod.unit}", 270f, currentY + 14f, paint)
                canvas.drawText("₹${prod.buyingPrice.toInt()}", 370f, currentY + 14f, paint)
                canvas.drawText("₹${itemValue.toInt()}", 450f, currentY + 14f, paint)

                currentY += 21f
            }
        }

        // Total Demand Box
        paint.color = Color.rgb(232, 245, 233)
        canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 24f, paint)
        paint.color = Color.rgb(27, 94, 32)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL ESTIMATED PROCUREMENT VALUE:", 240f, currentY + 16f, paint)
        canvas.drawText("₹${totalDemandValue.toInt()} / month", 450f, currentY + 16f, paint)

        // Field Intelligence Notes
        currentY += 40f
        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("4. FIELD INTELLIGENCE SUMMARY & LOGISTICS", 30f, currentY, paint)

        currentY += 12f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 65f, 6f, 6f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val noteText = survey.fieldIntelligenceSummary.ifBlank { "Standard ground observation recorded by Gram Shilpi." }
        canvas.drawText(noteText.take(110), 42f, currentY + 22f, paint)
        if (noteText.length > 110) {
            canvas.drawText(noteText.substring(110).take(110), 42f, currentY + 38f, paint)
        }
        val valNote = if (survey.validationRemarks.isNotBlank()) "Validation: ${survey.validationRemarks} (by ${survey.validatedBy})" else "Audit: Pending supervisor inspection"
        canvas.drawText(valNote.take(110), 42f, currentY + 54f, paint)

        // Sign-off Box
        currentY += 80f
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)

        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 9f
        canvas.drawText("Field Surveyor Signature: ${survey.surveyorName.ifBlank { userProfile.name }}", 40f, currentY + 25f, paint)
        canvas.drawText("Supervisor / Validator: ${survey.validatedBy.ifBlank { "DRI Planning Cell" }}", 340f, currentY + 25f, paint)
        canvas.drawText("Date: ${survey.dateString.ifBlank { dateDisplay }}", 40f, currentY + 40f, paint)
        canvas.drawText("Status: ${survey.status}", 340f, currentY + 40f, paint)

        // Footer
        paint.textSize = 8f
        paint.color = Color.rgb(140, 140, 140)
        canvas.drawText("DDU Gram Seva Kendra • Deendayal Research Institute • Page 1 of 1 • Confidential Field Document", 30f, pageHeight - 20f, paint)

        pdfDocument.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU_Field_Record_${survey.dduId}_$timestamp.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    /**
     * Generates a single Field Record Excel spreadsheet (.xls XML)
     */
    fun generateSurveyExcel(
        context: Context,
        record: SurveyWithDetails,
        userProfile: UserProfile
    ): File {
        val survey = record.survey
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU_Field_Record_${survey.dduId}_$timestamp.xls")

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

        // SHEET 1: Record Overview
        sb.append(""" <Worksheet ss:Name="Record_Profile">
  <Table>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">DDU FIELD OBSERVATION DOSSIER: ${escapeXml(survey.entityName)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">DDU Identifier:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.dduId)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Entity Name:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.entityName)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Entity Type:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.entityType)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Survey Category:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.surveyType)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Contact Person:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.contactPerson)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Contact Phone:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.contactNumber)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Village &amp; Tola:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.village)}, ${escapeXml(survey.tola)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Block &amp; District:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.block)}, ${escapeXml(survey.district)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">GPS Telemetry:</Data></Cell>
    <Cell><Data ss:Type="String">${survey.gpsLatitude}, ${survey.gpsLongitude} (Acc: ${survey.gpsAccuracyMeters}m)</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Validation Status:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.status)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Completeness Score:</Data></Cell>
    <Cell><Data ss:Type="Number">${survey.completenessScore}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Observation Summary:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(survey.fieldIntelligenceSummary)}</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Surveyor / Officer:</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(userProfile.name)} (${escapeXml(userProfile.designation)})</Data></Cell>
   </Row>
   <Row>
    <Cell ss:StyleID="BoldText"><Data ss:Type="String">Export Generated:</Data></Cell>
    <Cell><Data ss:Type="String">$dateDisplay</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
""")

        // SHEET 2: Demanded Products
        sb.append(""" <Worksheet ss:Name="Products_Demand">
  <Table>
   <Row ss:StyleID="Header">
    <Cell><Data ss:Type="String">Product Name</Data></Cell>
    <Cell><Data ss:Type="String">Category</Data></Cell>
    <Cell><Data ss:Type="String">Brand</Data></Cell>
    <Cell><Data ss:Type="String">Monthly Demand</Data></Cell>
    <Cell><Data ss:Type="String">Unit</Data></Cell>
    <Cell><Data ss:Type="String">Buying Price (INR)</Data></Cell>
    <Cell><Data ss:Type="String">Monthly Value (INR)</Data></Cell>
    <Cell><Data ss:Type="String">Frequency</Data></Cell>
    <Cell><Data ss:Type="String">Current Supplier</Data></Cell>
    <Cell><Data ss:Type="String">Supplier Source</Data></Cell>
   </Row>
""")

        for (prod in record.products) {
            val totalVal = prod.maxQuantity * prod.buyingPrice
            sb.append("""   <Row>
    <Cell><Data ss:Type="String">${escapeXml(prod.productName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(prod.category)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(prod.brand)}</Data></Cell>
    <Cell><Data ss:Type="Number">${prod.maxQuantity}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(prod.unit)}</Data></Cell>
    <Cell><Data ss:Type="Number">${prod.buyingPrice}</Data></Cell>
    <Cell><Data ss:Type="Number">$totalVal</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(prod.buyingFrequency)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(prod.currentSupplier)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(prod.currentSource)}</Data></Cell>
   </Row>
""")
        }

        sb.append("""  </Table>
 </Worksheet>
</Workbook>""")

        FileWriter(outputFile).use { writer ->
            writer.write(sb.toString())
        }
        return outputFile
    }

    /**
     * Generates a Single Field Record PowerPoint Presentation (.pptm / .ppt)
     */
    fun generateSurveyPpt(
        context: Context,
        record: SurveyWithDetails,
        userProfile: UserProfile,
        extension: String = "pptm"
    ): File {
        val survey = record.survey
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "DDU_Field_Record_${survey.dduId}_$timestamp.$extension"
        val outputFile = File(exportDir, filename)

        val pptContent = """<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:w="urn:schemas-microsoft-com:office:word"
      xmlns:p="urn:schemas-microsoft-com:office:powerpoint"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
<meta charset="utf-8">
<title>Field Record: ${survey.entityName} (${survey.dduId})</title>
<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; margin: 0; padding: 20px; color: #f8fafc; }
  .slide { background: #1e293b; border-radius: 12px; margin: 20px auto; max-width: 900px; padding: 40px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); page-break-after: always; }
  h1 { color: #4ade80; font-size: 28px; margin-top: 0; border-bottom: 2px solid #22c55e; padding-bottom: 12px; }
  h2 { color: #f59e0b; font-size: 22px; margin-top: 0; }
  p, li { font-size: 16px; line-height: 1.6; color: #cbd5e1; }
  .badge { background: #166534; color: #86efac; padding: 4px 10px; border-radius: 6px; font-weight: bold; font-size: 13px; display: inline-block; }
  .kpi-box { display: flex; gap: 15px; margin: 20px 0; }
  .kpi-card { background: #0f172a; border: 1px solid #334155; padding: 18px; border-radius: 8px; flex: 1; text-align: center; }
  .kpi-val { font-size: 28px; font-weight: bold; color: #4ade80; }
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
  <h1 style="font-size: 34px; margin-top: 15px;">DDU Gram Seva Kendra</h1>
  <h2>Field Intelligence Observation Dossier</h2>
  <div style="margin: 30px 0; padding: 20px; background: #0f172a; border-left: 4px solid #4ade80; border-radius: 4px;">
    <p style="font-size: 20px; font-weight: bold; color: #fff; margin: 0;">${survey.entityName}</p>
    <p style="margin: 6px 0 0 0; color: #94a3b8;">DDU ID: <b>${survey.dduId}</b> &bull; Type: <b>${survey.entityType}</b> &bull; Status: <b>${survey.status}</b></p>
    <p style="margin: 4px 0 0 0; color: #94a3b8;">Jurisdiction: ${survey.village} Village, ${survey.block} Block, ${survey.district} District</p>
  </div>
  <p><b>Lead Officer:</b> ${userProfile.name} (${userProfile.designation})</p>
  <p><b>Date:</b> $dateDisplay</p>
  <div class="footer">Presentation format (.${extension}) &bull; DDU Field Intelligence System</div>
</div>

<!-- SLIDE 2: ENTITY & GEOLOCATION -->
<div class="slide">
  <h1>Entity Profile &amp; Location Fix</h1>
  <h2>Operational Telemetry</h2>
  <div class="kpi-box">
    <div class="kpi-card">
      <div class="kpi-val">${survey.completenessScore}%</div>
      <div class="kpi-label">Completeness</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${if (survey.isSynced) "ONLINE" else "CACHED"}</div>
      <div class="kpi-label">Sync State</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">&plusmn;${survey.gpsAccuracyMeters.toInt()}m</div>
      <div class="kpi-label">GPS Accuracy</div>
    </div>
  </div>
  <ul>
    <li><b>Contact Person:</b> ${survey.contactPerson.ifBlank { "N/A" }} (${survey.contactNumber.ifBlank { "N/A" }})</li>
    <li><b>Coordinates:</b> ${survey.gpsLatitude}&deg; N, ${survey.gpsLongitude}&deg; E</li>
    <li><b>Village Cluster:</b> ${survey.village}, ${survey.tola} (${survey.block} Block)</li>
    <li><b>Survey Category:</b> ${survey.surveyType}</li>
    <li><b>Validation Notes:</b> ${survey.validationRemarks.ifBlank { "Verified on ground by DRI survey team." }}</li>
  </ul>
  <div class="footer">DDU Field Intelligence &bull; Chitrakoot DRI</div>
</div>

<!-- SLIDE 3: DEMAND & PROCUREMENT -->
<div class="slide">
  <h1>Procurement Demand &amp; Sourcing</h1>
  <h2>Identified Product Inflow (${record.products.size} Items)</h2>
  <table>
    <tr>
      <th>Product</th>
      <th>Category</th>
      <th>Demand / Unit</th>
      <th>Buying Price</th>
      <th>Current Sourcing</th>
    </tr>
${record.products.joinToString("\n") { prod ->
    "    <tr><td><b>${prod.productName}</b></td><td>${prod.category}</td><td>${prod.maxQuantity.toInt()} ${prod.unit}</td><td>&#8377;${prod.buyingPrice.toInt()}</td><td>${prod.currentSource}</td></tr>"
}}
  </table>
  <div class="footer">DDU Field Intelligence &bull; Commercial Demand Synthesis</div>
</div>

<!-- SLIDE 4: RECOMMENDATIONS & NEXT STEPS -->
<div class="slide">
  <h1>Strategic Synthesis &amp; Next Steps</h1>
  <h2>Enterprise Linkage Roadmap</h2>
  <div style="background: #0f172a; padding: 20px; border-radius: 8px; margin-top: 15px;">
    <p><b>Observation Summary:</b></p>
    <p style="color: #94a3b8;">${survey.fieldIntelligenceSummary.ifBlank { "Field entity demonstrates viable recurring consumer and institutional procurement demand suitable for local cluster aggregation." }}</p>
  </div>
  <ul style="margin-top: 20px;">
    <li>Incorporate procurement volumes into <b>Stage 1 Demand Synthesis</b> pipeline.</li>
    <li>Assess village production cluster feasibility in <b>Stage 2 DDU Assessment</b>.</li>
    <li>Link with local women SHGs through <b>DSDC Sakhya Incubation Network</b>.</li>
  </ul>
  <div class="footer">DDU Gram Seva Kendra &bull; Deendayal Research Institute</div>
</div>

</body>
</html>"""

        FileWriter(outputFile).use { writer ->
            writer.write(pptContent)
        }
        return outputFile
    }

    // =========================================================================
    // 2. DDU 2 (STAGE 2 ASSESSMENT) EXPORTS (PDF / EXCEL / PPTM)
    // =========================================================================

    /**
     * Generates a single Stage 2 DDU Assessment Dossier PDF
     */
    fun generateStage2Pdf(
        context: Context,
        assessment: VillageProductionAssessmentEntity,
        userProfile: UserProfile
    ): File {
        val pdfDocument = PdfDocument()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        val pageWidth = 595
        val pageHeight = 842
        val paint = Paint().apply { isAntiAlias = true }

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Header Background
        paint.color = Color.rgb(27, 94, 32)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

        // Accent strip
        paint.color = Color.rgb(245, 127, 23)
        canvas.drawRect(0f, 110f, pageWidth.toFloat(), 115f, paint)

        // Header text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DEENDAYAL RESEARCH INSTITUTE • CHITRAKOOT", 30f, 32f, paint)

        paint.textSize = 16f
        canvas.drawText("STAGE 2: VILLAGE PRODUCTION ASSESSMENT DOSSIER", 30f, 58f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("DDU 2 Feasibility Dossier • Product: ${assessment.productName} • Vatika: ${assessment.vatika}", 30f, 78f, paint)
        canvas.drawText("Filled by: ${assessment.filledBy} | Date: ${assessment.assessmentDate} | User: ${userProfile.name}", 30f, 95f, paint)

        var currentY = 135f

        // Part A: Basic Information Card
        paint.color = Color.rgb(240, 245, 240)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 100f, 8f, 8f, paint)

        paint.color = Color.rgb(27, 94, 32)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART A — BASIC PRODUCT & VALUE CHAIN INFORMATION", 45f, currentY + 22f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Target Product: ${assessment.productName} | Gram Vatika: ${assessment.vatika}", 45f, currentY + 40f, paint)
        canvas.drawText("Local Demand Established: ${if (assessment.demandFromLocalSurvey) "YES (Survey Backed)" else "NO"}", 45f, currentY + 54f, paint)
        canvas.drawText("Raw Material: ${assessment.mainRawMaterialNeeded} (Available locally: ${if (assessment.rawMaterialAvailableLocally) "Yes" else "No"})", 45f, currentY + 68f, paint)
        canvas.drawText("Trained Women Artisans Available: ${if (assessment.trainedWomenAvailable) "YES" else "NO"} | Training Required: ${if (assessment.trainingNeededForQuality) "YES (${assessment.trainingKindNeeded.take(30)})" else "NO"}", 45f, currentY + 82f, paint)
        canvas.drawText("External Technical Support: ${assessment.externalSupportNeeded.ifBlank { "Not required" }}", 45f, currentY + 95f, paint)

        // Part B & C: Priority & Critical Feasibility
        currentY += 115f
        val isCriticalPass = assessment.criticalityRatingPass
        paint.color = if (isCriticalPass) Color.rgb(232, 245, 233) else Color.rgb(255, 235, 238)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 95f, 8f, 8f, paint)

        paint.color = if (isCriticalPass) Color.rgb(27, 94, 32) else Color.rgb(198, 40, 40)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART B & C — CRITICAL FEASIBILITY & PRIORITY RATING", 45f, currentY + 22f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Priority Level Assigned: Priority ${assessment.priorityLevel} / 6", 45f, currentY + 40f, paint)
        canvas.drawText("Critical Feasibility Status: ${if (isCriticalPass) "PASS (Mandatory checks satisfied)" else "FAIL (Critical constraints identified)"}", 45f, currentY + 55f, paint)
        canvas.drawText("Packaging Available: ${if (assessment.packagingLocallyAvailable) "Yes" else "No"} | Production in Village: ${if (assessment.canBeProducedInVillage) "Yes" else "No"}", 45f, currentY + 70f, paint)
        canvas.drawText("Proceed to Next Stage: ${if (assessment.criticalProceedNext) "YES" else "NO"} | Critical Status: ${if (assessment.criticalityRatingPass) "PASS" else "FAIL"}", 45f, currentY + 85f, paint)

        // Part D: Readiness Evaluation
        currentY += 110f
        paint.color = Color.rgb(243, 244, 246)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 90f, 8f, 8f, paint)

        paint.color = Color.rgb(31, 41, 55)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PART D & E — PRODUCTION READINESS (${assessment.readinessScore}/7)", 45f, currentY + 22f, paint)

        paint.color = Color.rgb(55, 65, 81)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("1. Material Cost Stability: ${if (assessment.rawMaterialCostStability == 1) "Stable" else "Fluctuates"} | 2. Trainer Ready: ${if (assessment.trainerAvailable == 1) "Yes" else "No"}", 45f, currentY + 40f, paint)
        canvas.drawText("3. Raw Material Route: ${if (assessment.productionOrRawMaterialAvailable == 1) "Yes" else "No"} | 4. Workspace: ${if (assessment.workspaceAvailable == 1) "Yes" else "No"}", 45f, currentY + 54f, paint)
        canvas.drawText("5. Electricity: ${if (assessment.electricityAvailable == 1) "Yes" else "No"} | 6. Water: ${if (assessment.waterAvailable == 1) "Yes" else "No"} | 7. Storage: ${if (assessment.storageAvailable == 1) "Yes" else "No"}", 45f, currentY + 68f, paint)
        canvas.drawText("Sample Tested Locally: ${if (assessment.isSampleAvailable) "YES" else "NO"} | Sampling Checked: ${if (assessment.samplingChecked) "YES" else "NO"}", 45f, currentY + 82f, paint)

        // Part E: SWSM Final Decision
        currentY += 105f
        val isSelected = isCriticalPass && (assessment.samplingChecked || assessment.readinessScore >= 5)
        paint.color = if (isSelected) Color.rgb(232, 245, 233) else Color.rgb(255, 243, 224)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 85f, 8f, 8f, paint)

        paint.color = if (isSelected) Color.rgb(27, 94, 32) else Color.rgb(230, 81, 0)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val decisionTitle = if (isSelected) "FINAL DECISION: SELECTED FOR DDU PRODUCTION CLUSTER" else if (!isCriticalPass) "FINAL DECISION: DROPPED (FEASIBILITY CONSTRAINTS)" else "FINAL DECISION: RECOMMENDED WITH SPECIAL SUPPORT"
        canvas.drawText(decisionTitle, 45f, currentY + 24f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Committee Decision Category: ${if (isSelected) "Tier 1 Direct Incubation" else "Tier 2 Capacity Building Pipeline"}", 45f, currentY + 44f, paint)
        canvas.drawText("SWSM Committee Notes: ${assessment.swsmDecisionNotes.ifBlank { "Approved for village cluster manufacturing and SHG onboarding." }}", 45f, currentY + 59f, paint)
        canvas.drawText("Assessment Date: ${assessment.assessmentDate} | Evaluator: ${assessment.filledBy}", 45f, currentY + 74f, paint)

        // Footer & Sign-off
        currentY += 100f
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)

        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 9f
        canvas.drawText("Field Fellow Signature: _______________________", 40f, currentY + 25f, paint)
        canvas.drawText("SWSM Project Director: _______________________", 340f, currentY + 25f, paint)

        paint.textSize = 8f
        canvas.drawText("Stage 2 DDU Village Production Feasibility • DRI Chitrakoot • Page 1 of 1", 30f, pageHeight - 20f, paint)

        pdfDocument.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU2_Assessment_${assessment.productName.replace(" ", "_")}_$timestamp.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    /**
     * Generates a single Stage 2 DDU Assessment Excel file (.xls)
     */
    fun generateStage2Excel(
        context: Context,
        assessment: VillageProductionAssessmentEntity,
        userProfile: UserProfile
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "DDU2_Assessment_${assessment.productName.replace(" ", "_")}_$timestamp.xls")

        val isCriticalPass = assessment.criticalityRatingPass
        val isSelected = isCriticalPass && (assessment.samplingChecked || assessment.readinessScore >= 5)

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Styles>
  <Style ss:ID="Header">
   <Font ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#1B5E20" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="BoldText">
   <Font ss:Bold="1"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="DDU2_Assessment">
  <Table>
   <Row><Cell ss:StyleID="Header"><Data ss:Type="String">DDU 2: STAGE 2 VILLAGE PRODUCTION ASSESSMENT</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Product Name:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(assessment.productName)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Vatika / Cluster:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(assessment.vatika)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Filled By:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(assessment.filledBy)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Assessment Date:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(assessment.assessmentDate)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Local Demand Surveyed:</Data></Cell><Cell><Data ss:Type="String">${if (assessment.demandFromLocalSurvey) "YES" else "NO"}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Raw Material Needed:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(assessment.mainRawMaterialNeeded)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Raw Material Local:</Data></Cell><Cell><Data ss:Type="String">${if (assessment.rawMaterialAvailableLocally) "YES" else "NO"}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Trained Women Available:</Data></Cell><Cell><Data ss:Type="String">${if (assessment.trainedWomenAvailable) "YES" else "NO"}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Priority Level (1-6):</Data></Cell><Cell><Data ss:Type="Number">${assessment.priorityLevel}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Critical Feasibility:</Data></Cell><Cell><Data ss:Type="String">${if (isCriticalPass) "PASS" else "FAIL"}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Readiness Score (out of 7):</Data></Cell><Cell><Data ss:Type="Number">${assessment.readinessScore}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Sampling Checked:</Data></Cell><Cell><Data ss:Type="String">${if (assessment.samplingChecked) "YES" else "NO"}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Final SWSM Selection:</Data></Cell><Cell><Data ss:Type="String">${if (isSelected) "SELECTED" else if (!isCriticalPass) "DROPPED" else "SUPPORT NEEDED"}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">SWSM Decision Notes:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(assessment.swsmDecisionNotes)}</Data></Cell></Row>
  </Table>
 </Worksheet>
</Workbook>""")

        FileWriter(outputFile).use { it.write(sb.toString()) }
        return outputFile
    }

    /**
     * Generates a Stage 2 DDU Assessment Presentation (.pptm / .ppt)
     */
    fun generateStage2Ppt(
        context: Context,
        assessment: VillageProductionAssessmentEntity,
        userProfile: UserProfile,
        extension: String = "pptm"
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "DDU2_Assessment_${assessment.productName.replace(" ", "_")}_$timestamp.$extension"
        val outputFile = File(exportDir, filename)

        val isCriticalPass = assessment.criticalityRatingPass
        val isSelected = isCriticalPass && (assessment.samplingChecked || assessment.readinessScore >= 5)

        val pptContent = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Stage 2 DDU Assessment: ${assessment.productName}</title>
<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; margin: 0; padding: 20px; color: #f8fafc; }
  .slide { background: #1e293b; border-radius: 12px; margin: 20px auto; max-width: 900px; padding: 40px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); page-break-after: always; }
  h1 { color: #4ade80; font-size: 28px; margin-top: 0; border-bottom: 2px solid #22c55e; padding-bottom: 12px; }
  h2 { color: #f59e0b; font-size: 22px; margin-top: 0; }
  p, li { font-size: 16px; line-height: 1.6; color: #cbd5e1; }
  .badge { background: #166534; color: #86efac; padding: 4px 10px; border-radius: 6px; font-weight: bold; font-size: 13px; display: inline-block; }
  .kpi-box { display: flex; gap: 15px; margin: 20px 0; }
  .kpi-card { background: #0f172a; border: 1px solid #334155; padding: 18px; border-radius: 8px; flex: 1; text-align: center; }
  .kpi-val { font-size: 28px; font-weight: bold; color: #4ade80; }
  .kpi-label { font-size: 13px; color: #94a3b8; text-transform: uppercase; }
  .footer { margin-top: 30px; font-size: 12px; color: #64748b; border-top: 1px solid #334155; padding-top: 10px; text-align: right; }
</style>
</head>
<body>

<div class="slide">
  <span class="badge">DEENDAYAL RESEARCH INSTITUTE</span>
  <h1>Stage 2: DDU Village Production Feasibility</h1>
  <h2>${assessment.productName}</h2>
  <div style="background: #0f172a; padding: 20px; border-radius: 8px; margin: 25px 0;">
    <p><b>Gram Vatika:</b> ${assessment.vatika} &bull; <b>Date:</b> ${assessment.assessmentDate}</p>
    <p><b>Evaluation Lead:</b> ${assessment.filledBy}</p>
    <p><b>Status:</b> ${if (isSelected) "<span style='color:#4ade80;'>SELECTED FOR DDU HUB</span>" else if (!isCriticalPass) "<span style='color:#f87171;'>DROPPED</span>" else "<span style='color:#fbbf24;'>REQUIRES SWSM SUPPORT</span>"}</p>
  </div>
  <div class="footer">Presentation (.${extension}) &bull; DDU Field Intelligence System</div>
</div>

<div class="slide">
  <h1>Feasibility &amp; Readiness Dashboard</h1>
  <h2>Production Capability Evaluation</h2>
  <div class="kpi-box">
    <div class="kpi-card">
      <div class="kpi-val">P${assessment.priorityLevel}</div>
      <div class="kpi-label">Priority Level</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${assessment.readinessScore}/7</div>
      <div class="kpi-label">Readiness Score</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${if (isCriticalPass) "PASS" else "FAIL"}</div>
      <div class="kpi-label">Critical Checks</div>
    </div>
  </div>
  <ul>
    <li><b>Local Raw Material:</b> ${if (assessment.rawMaterialAvailableLocally) "Available Locally" else "Inflow from Outside Needed"}</li>
    <li><b>Artisan Base:</b> ${if (assessment.trainedWomenAvailable) "Skilled women available" else "Mobilization needed"}</li>
    <li><b>Training Support:</b> ${if (assessment.trainingNeededForQuality) assessment.trainingKindNeeded else "Sufficient core skill ready"}</li>
    <li><b>Sample Quality Tested:</b> ${if (assessment.samplingChecked) "Sample Tested & Approved" else "Sample verification pending"}</li>
  </ul>
  <div class="footer">DDU Gram Seva Kendra &bull; DRI Chitrakoot</div>
</div>

<div class="slide">
  <h1>SWSM Committee Decision</h1>
  <h2>Cluster Incubation Strategy</h2>
  <div style="background: #0f172a; padding: 20px; border-radius: 8px; margin-top: 15px;">
    <p><b>Decision Notes:</b></p>
    <p style="color: #cbd5e1;">${assessment.swsmDecisionNotes.ifBlank { "Approved for integration into Stage 3 Gram Seva Kendra enterprise rollout." }}</p>
  </div>
  <div class="footer">Stage 2 DDU &bull; Deendayal Research Institute</div>
</div>

</body>
</html>"""

        FileWriter(outputFile).use { it.write(pptContent) }
        return outputFile
    }

    // =========================================================================
    // 3. SAKHYA WOMEN ENTERPRISE (FORM F1) EXPORTS (PDF / EXCEL / PPTM)
    // =========================================================================

    /**
     * Generates a single Sakhya Screening Dossier PDF (Form F1)
     */
    fun generateSakhyaPdf(
        context: Context,
        screening: SakhyaScreeningEntity,
        userProfile: UserProfile
    ): File {
        val pdfDocument = PdfDocument()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        val pageWidth = 595
        val pageHeight = 842
        val paint = Paint().apply { isAntiAlias = true }

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Header Background (Deep Forest / Emerald)
        paint.color = Color.rgb(22, 77, 62)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

        // Accent strip (Gold / Amber)
        paint.color = Color.rgb(217, 119, 6)
        canvas.drawRect(0f, 110f, pageWidth.toFloat(), 115f, paint)

        // Header text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DRI • DEENDAYAL SHODH SANSTHAN • CHITRAKOOT", 30f, 32f, paint)

        paint.textSize = 15f
        canvas.drawText("SAKHYA WOMEN ENTERPRISE DOSSIER (FORM F1)", 30f, 58f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Prospect ID: ${screening.screeningId} • Entrepreneur: ${screening.entrepreneurName}", 30f, 78f, paint)
        canvas.drawText("Evaluator: ${screening.fieldFellowName} | Linked Survey: ${screening.linkedDduId.ifBlank { "General Cluster" }} | $dateDisplay", 30f, 95f, paint)

        var currentY = 135f

        // Entrepreneur Profile Card
        paint.color = Color.rgb(240, 247, 244)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 105f, 8f, 8f, paint)

        paint.color = Color.rgb(22, 77, 62)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. PROSPECT & ENTERPRISE PROFILE", 45f, currentY + 22f, paint)

        paint.color = Color.rgb(33, 33, 33)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Entrepreneur Name: ${screening.entrepreneurName} | Years in Business: ${screening.yearsInBusiness} yrs", 45f, currentY + 40f, paint)
        canvas.drawText("Value Chain: ${screening.productAndValueChain} | Status: ${screening.status}", 45f, currentY + 54f, paint)
        canvas.drawText("Marketing: ${screening.dedicatedMarketingMember} | Market Level: ${screening.marketLevelsServed.take(35)}", 45f, currentY + 68f, paint)
        canvas.drawText("Monthly Profit: ₹${screening.currentMonthlyProfitability.toInt()}/month | Break-Even: ${screening.timeToBreakEven}", 45f, currentY + 82f, paint)
        canvas.drawText("Screening Date: ${screening.screeningDate} | Evaluator: ${screening.fieldFellowName}", 45f, currentY + 96f, paint)

        // Section A & B: Drive & Skills
        currentY += 120f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 95f, 8f, 8f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. CAPABILITY & MOTIVATION ASSESSMENT", 45f, currentY + 22f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Professional Training: ${if (screening.receivedProfessionalTraining) "YES" else "NO"} | Started Via: ${screening.howGotIntoBusiness}", 45f, currentY + 38f, paint)
        canvas.drawText("Expansion Aspirations: ${screening.expansionAspirations} | Mindset: ${screening.mindsetOrientation}", 45f, currentY + 52f, paint)
        canvas.drawText("Record Keeping: Financial [${screening.recordFinancial}] | Customer [${screening.recordCustomer}] | Sales [${screening.recordSalesData}]", 45f, currentY + 66f, paint)
        canvas.drawText("Batch Frequency: ${screening.batchFrequency} (${screening.cyclesPerYear} cycles/yr) | Input Selection: ${screening.inputSupplySelectionBasis}", 45f, currentY + 80f, paint)

        // Readiness Score Box
        currentY += 110f
        val isReady = screening.readinessScore >= 75
        paint.color = if (isReady) Color.rgb(232, 245, 233) else Color.rgb(255, 243, 224)
        canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 75f, 8f, 8f, paint)

        paint.color = if (isReady) Color.rgb(21, 128, 61) else Color.rgb(217, 119, 6)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val scoreCategory = if (screening.readinessScore >= 75) "UDYAMI READY (TIER 1)" else if (screening.readinessScore >= 50) "DEVELOPING ENTERPRISE (TIER 2)" else "EARLY STAGE / FOUNDATIONAL"
        canvas.drawText("READINESS SCORE: ${screening.readinessScore}/100 — $scoreCategory", 45f, currentY + 28f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Recommended Incubation: ${if (isReady) "Immediate equipment grant & DDU market linkage" else "Vocational skill training & micro-credit orientation"}", 45f, currentY + 50f, paint)
        canvas.drawText("Field Observations: Candidate demonstrates strong community standing in ${screening.productAndValueChain}.", 45f, currentY + 65f, paint)

        // Sign-off
        currentY += 95f
        paint.color = Color.rgb(200, 200, 200)
        canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)

        paint.color = Color.rgb(100, 100, 100)
        paint.textSize = 9f
        canvas.drawText("Field Fellow Signature: ${screening.fieldFellowName}", 40f, currentY + 25f, paint)
        canvas.drawText("DSDC Sakhya Cell: Chitrakoot DRI", 340f, currentY + 25f, paint)

        paint.textSize = 8f
        canvas.drawText("DSDC Sakhya Women Enterprise Screening • Form F1 • Confidential Field Document", 30f, pageHeight - 20f, paint)

        pdfDocument.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "Sakhya_Dossier_${screening.entrepreneurName.replace(" ", "_")}_$timestamp.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    /**
     * Generates a single Sakhya Screening Excel file (.xls)
     */
    fun generateSakhyaExcel(
        context: Context,
        screening: SakhyaScreeningEntity,
        userProfile: UserProfile
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(exportDir, "Sakhya_Screening_${screening.entrepreneurName.replace(" ", "_")}_$timestamp.xls")

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
 <Styles>
  <Style ss:ID="Header">
   <Font ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#1B4D3E" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="BoldText"><Font ss:Bold="1"/></Style>
 </Styles>
 <Worksheet ss:Name="Sakhya_F1">
  <Table>
   <Row><Cell ss:StyleID="Header"><Data ss:Type="String">DSDC SAKHYA WOMEN ENTERPRISE SCREENING (FORM F1)</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Screening ID:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.screeningId)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Entrepreneur Name:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.entrepreneurName)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Value Chain:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.productAndValueChain)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Linked Survey DDU ID:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.linkedDduId)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Years in Business:</Data></Cell><Cell><Data ss:Type="Number">${screening.yearsInBusiness}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Monthly Profitability (INR):</Data></Cell><Cell><Data ss:Type="Number">${screening.currentMonthlyProfitability}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Dedicated Marketing:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.dedicatedMarketingMember)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Market Levels Served:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.marketLevelsServed)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Expansion Aspirations:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.expansionAspirations)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Mindset Orientation:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.mindsetOrientation)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Readiness Score (0-100):</Data></Cell><Cell><Data ss:Type="Number">${screening.readinessScore}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Field Fellow:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.fieldFellowName)}</Data></Cell></Row>
   <Row><Cell ss:StyleID="BoldText"><Data ss:Type="String">Status:</Data></Cell><Cell><Data ss:Type="String">${escapeXml(screening.status)}</Data></Cell></Row>
  </Table>
 </Worksheet>
</Workbook>""")

        FileWriter(outputFile).use { it.write(sb.toString()) }
        return outputFile
    }

    /**
     * Generates a Sakhya Screening Presentation (.pptm / .ppt)
     */
    fun generateSakhyaPpt(
        context: Context,
        screening: SakhyaScreeningEntity,
        userProfile: UserProfile,
        extension: String = "pptm"
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "Sakhya_Presentation_${screening.entrepreneurName.replace(" ", "_")}_$timestamp.$extension"
        val outputFile = File(exportDir, filename)

        val pptContent = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Sakhya Prospect: ${screening.entrepreneurName}</title>
<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; margin: 0; padding: 20px; color: #f8fafc; }
  .slide { background: #1e293b; border-radius: 12px; margin: 20px auto; max-width: 900px; padding: 40px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); page-break-after: always; }
  h1 { color: #34d399; font-size: 28px; margin-top: 0; border-bottom: 2px solid #10b981; padding-bottom: 12px; }
  h2 { color: #f59e0b; font-size: 22px; margin-top: 0; }
  p, li { font-size: 16px; line-height: 1.6; color: #cbd5e1; }
  .badge { background: #064e3b; color: #6ee7b7; padding: 4px 10px; border-radius: 6px; font-weight: bold; font-size: 13px; display: inline-block; }
  .kpi-box { display: flex; gap: 15px; margin: 20px 0; }
  .kpi-card { background: #0f172a; border: 1px solid #334155; padding: 18px; border-radius: 8px; flex: 1; text-align: center; }
  .kpi-val { font-size: 28px; font-weight: bold; color: #34d399; }
  .kpi-label { font-size: 13px; color: #94a3b8; text-transform: uppercase; }
  .footer { margin-top: 30px; font-size: 12px; color: #64748b; border-top: 1px solid #334155; padding-top: 10px; text-align: right; }
</style>
</head>
<body>

<div class="slide">
  <span class="badge">DSDC SAKHYA INITIATIVE</span>
  <h1>Women Micro-Enterprise Incubation Pitch</h1>
  <h2>${screening.entrepreneurName}</h2>
  <div style="background: #0f172a; padding: 20px; border-radius: 8px; margin: 25px 0;">
    <p><b>Product / Enterprise:</b> ${screening.productAndValueChain}</p>
    <p><b>Linked DDU Survey:</b> ${screening.linkedDduId.ifBlank { "Direct Prospect" }} &bull; <b>Years in Business:</b> ${screening.yearsInBusiness} yrs</p>
    <p><b>Readiness Tier:</b> ${if (screening.readinessScore >= 75) "<span style='color:#34d399;'>UDYAMI READY</span>" else "<span style='color:#fbbf24;'>DEVELOPING</span>"}</p>
  </div>
  <div class="footer">Presentation (.${extension}) &bull; DSDC Sakhya &bull; DRI Chitrakoot</div>
</div>

<div class="slide">
  <h1>Entrepreneur Evaluation Scores</h1>
  <h2>Form F1 Assessment Metric</h2>
  <div class="kpi-box">
    <div class="kpi-card">
      <div class="kpi-val">${screening.readinessScore}/100</div>
      <div class="kpi-label">Overall Readiness</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">${screening.yearsInBusiness} yrs</div>
      <div class="kpi-label">Experience</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-val">&#8377;${screening.currentMonthlyProfitability.toInt()}</div>
      <div class="kpi-label">Monthly Profit</div>
    </div>
  </div>
  <ul>
    <li><b>Marketing Approach:</b> ${screening.dedicatedMarketingMember}</li>
    <li><b>Market Levels:</b> ${screening.marketLevelsServed}</li>
    <li><b>Incubation Strategy:</b> Direct linkage with DDU Gram Seva Kendra supply chains.</li>
  </ul>
  <div class="footer">DSDC Sakhya Women Enterprise &bull; DRI Chitrakoot</div>
</div>

</body>
</html>"""

        FileWriter(outputFile).use { it.write(pptContent) }
        return outputFile
    }

    // =========================================================================
    // 4. SHARING & SYSTEM INTENTS
    // =========================================================================

    fun openFile(context: Context, file: File) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".pptm", ignoreCase = true) || file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
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
            Toast.makeText(context, "No app found to open .${file.extension} file", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFile(context: Context, file: File, title: String) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".pptm", ignoreCase = true) || file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
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

    fun shareViaWhatsApp(context: Context, file: File, caption: String) {
        val uri = getFileUri(context, file)
        val mimeType = when {
            file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            file.name.endsWith(".pptm", ignoreCase = true) || file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to normal chooser if WhatsApp isn't installed
            shareFile(context, file, "Share via WhatsApp")
        }
    }

    fun shareViaEmail(context: Context, file: File, subject: String, body: String) {
        val uri = getFileUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            shareFile(context, file, subject)
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
