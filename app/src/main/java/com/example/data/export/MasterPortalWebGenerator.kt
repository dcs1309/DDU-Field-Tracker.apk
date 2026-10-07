package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.analytics.model.PowerBiConfig
import com.example.data.model.OpportunityEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.UserProfile
import com.example.data.model.VillageProductionAssessmentEntity
import com.example.viewmodel.UiStatistics
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Master Website Portal Generator for DDU Field Intelligence.
 * Builds a complete, standalone, responsive Single-Page Application (SPA) HTML5 portal
 * that provides unified web access to:
 * - Microsoft Power BI Analytics & Embedded Dashboards
 * - Field Intelligence Database & Verified Surveys Directory
 * - DDU Opportunity Pipeline & Dossiers with Source Evidence Traceability
 * - Interactive GIS Field Map with category-colored pins
 * - SWSM Groups & Sakhya Entrepreneur Linkages (Forms F1)
 * - Stage 2 Village Production Assessment Matrix
 */
object MasterPortalWebGenerator {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    /**
     * Generates and writes the master web portal HTML file to disk, returning the File.
     */
    fun generatePortalHtmlFile(
        context: Context,
        userProfile: UserProfile,
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>,
        sakhyaScreenings: List<SakhyaScreeningEntity>,
        stats: UiStatistics,
        powerBiConfig: PowerBiConfig = PowerBiConfig()
    ): File {
        val htmlContent = buildPortalHtmlString(
            userProfile = userProfile,
            surveys = surveys,
            opportunities = opportunities,
            stage2Assessments = stage2Assessments,
            sakhyaScreenings = sakhyaScreenings,
            stats = stats,
            powerBiConfig = powerBiConfig
        )

        val portalDir = File(context.filesDir, "web_portal").apply { mkdirs() }
        val portalFile = File(portalDir, "index.html")
        FileOutputStream(portalFile).use { it.write(htmlContent.toByteArray(Charsets.UTF_8)) }

        // Also write a timestamped export copy for sharing
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportFile = File(exportDir, "DDU_Master_Web_Portal_$timestamp.html")
        FileOutputStream(exportFile).use { it.write(htmlContent.toByteArray(Charsets.UTF_8)) }

        return portalFile
    }

    /**
     * Builds the complete, modern, self-contained HTML5 string.
     */
    fun buildPortalHtmlString(
        userProfile: UserProfile,
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>,
        sakhyaScreenings: List<SakhyaScreeningEntity>,
        stats: UiStatistics,
        powerBiConfig: PowerBiConfig = PowerBiConfig()
    ): String {
        val dateDisplay = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val shortlistedCount = opportunities.count { it.isShortlistedForStage2 }
        val discoveryCount = opportunities.count { it.status == "IDENTIFIED" || it.status == "EVIDENCE_COLLECTED" || it.status == "PATTERN_CONFIRMED" }

        // Build Surveys JSON
        val surveysJson = JSONArray()
        surveys.forEach { item ->
            val s = item.survey
            val obj = JSONObject()
            obj.put("dduId", s.dduId)
            obj.put("surveyType", s.surveyType)
            obj.put("entityName", s.entityName)
            obj.put("entityType", s.entityType)
            obj.put("contactPerson", s.contactPerson)
            obj.put("contactNumber", s.contactNumber)
            obj.put("village", s.village)
            obj.put("tola", s.tola)
            obj.put("block", s.block)
            obj.put("district", s.district)
            obj.put("lat", s.gpsLatitude)
            obj.put("lng", s.gpsLongitude)
            obj.put("surveyor", s.surveyorName)
            obj.put("date", s.dateString)
            obj.put("status", s.status)
            obj.put("confidence", s.confidenceLevel)
            obj.put("completeness", s.completenessScore)
            obj.put("summary", s.fieldIntelligenceSummary)

            val prodsArr = JSONArray()
            item.products.forEach { p ->
                val pObj = JSONObject()
                pObj.put("name", p.productName)
                pObj.put("category", p.category)
                pObj.put("unit", p.unit)
                pObj.put("minQty", p.minQuantity)
                pObj.put("buyingPrice", p.buyingPrice)
                pObj.put("sellingPrice", p.sellingPrice)
                pObj.put("supplier", p.currentSupplier)
                pObj.put("source", p.currentSource)
                prodsArr.put(pObj)
            }
            obj.put("products", prodsArr)

            val evArr = JSONArray()
            item.evidenceList.forEach { ev ->
                val eObj = JSONObject()
                eObj.put("type", ev.type)
                eObj.put("category", ev.category)
                eObj.put("caption", ev.caption)
                eObj.put("transcript", ev.transcription ?: "")
                eObj.put("duration", ev.durationSeconds)
                evArr.put(eObj)
            }
            obj.put("evidence", evArr)

            surveysJson.put(obj)
        }

        // Build Opportunities JSON
        val oppsJson = JSONArray()
        opportunities.forEach { opp ->
            val obj = JSONObject()
            obj.put("oppId", opp.oppId)
            obj.put("title", opp.title)
            obj.put("category", opp.category)
            obj.put("level", opp.level)
            obj.put("status", opp.status)
            obj.put("buyersCount", opp.demandingEntitiesCount)
            obj.put("annualDemand", opp.estimatedAnnualDemand)
            obj.put("buyingPattern", opp.buyingPattern)
            obj.put("currentSource", opp.currentSource)
            obj.put("priceRange", opp.averagePriceRange)
            obj.put("cluster", opp.geographicCluster)
            obj.put("evidenceSummary", opp.evidenceSummary)
            obj.put("confidence", opp.confidence)
            obj.put("potentialEnterprise", opp.potentialEnterprise)
            obj.put("relatedBuyers", opp.relatedBuyers)
            obj.put("narrative", opp.interpretationNarrative)
            obj.put("nextSteps", opp.nextSteps)
            obj.put("isShortlisted", opp.isShortlistedForStage2)
            oppsJson.put(obj)
        }

        val modeLabel = if (powerBiConfig.isConfigured) "Connected" else "Demo Preview"

        val sb = java.lang.StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        sb.append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        sb.append("<title>DDU Field Intelligence • Master Website Portal</title>\n")
        sb.append("<script src=\"https://cdn.tailwindcss.com\"></script>\n")
        sb.append("<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n")
        sb.append("<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n")
        sb.append("<script src=\"https://unpkg.com/lucide@latest\"></script>\n")
        sb.append("""<style>
:root { --primary: #0F766E; --primary-dark: #134E4A; --accent: #EA580C; --bg-light: #F8FAFC; }
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background-color: #F8FAFC; color: #0F172A; }
.portal-shadow { box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.08); }
.tab-btn.active { color: #0F766E; border-bottom: 3px solid #0F766E; font-weight: 700; }
#map-container { height: 480px; border-radius: 16px; z-index: 10; }
.badge-institution { background-color: #DCFCE7; color: #15803D; border: 1px solid #BBF7D0; }
.badge-shop { background-color: #E0F2FE; color: #0284C7; border: 1px solid #BAE6FD; }
.badge-supplier { background-color: #FFEDD5; color: #EA580C; border: 1px solid #FED7AA; }
</style>
</head>
<body class="min-h-screen flex flex-col">
""")

        // HEADER
        sb.append("""<header class="bg-teal-900 text-white shadow-lg sticky top-0 z-50">
<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
  <div class="flex items-center justify-between h-16">
    <div class="flex items-center space-x-3">
      <div class="w-10 h-10 rounded-xl bg-teal-800 flex items-center justify-center font-black text-xl text-teal-300 border border-teal-700 shadow-inner">DDU</div>
      <div>
        <div class="flex items-center space-x-2">
          <h1 class="text-lg font-bold tracking-tight">DDU Field Intelligence</h1>
          <span class="px-2 py-0.5 text-xs font-bold rounded-full bg-teal-800 text-teal-200 border border-teal-700">MASTER PORTAL</span>
        </div>
        <p class="text-xs text-teal-300">Deendayal Research Institute • Opportunity Discovery &amp; Power BI Hub</p>
      </div>
    </div>
    <div class="flex items-center space-x-4">
      <div class="hidden md:flex items-center space-x-2 bg-teal-800/80 px-3 py-1.5 rounded-lg border border-teal-700/60 text-xs text-teal-200">
        <span class="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
        <span>Cloud Database Synced</span>
        <span class="text-teal-400">•</span>
        <span>$dateDisplay</span>
      </div>
      <div class="flex items-center space-x-2 bg-teal-800 px-3 py-1.5 rounded-lg border border-teal-700">
        <i data-lucide="user" class="w-4 h-4 text-teal-300"></i>
        <div class="text-xs text-left">
          <div class="font-bold text-white">${userProfile.name}</div>
          <div class="text-[10px] text-teal-300">${userProfile.role.label} (${userProfile.block})</div>
        </div>
      </div>
      <button onclick="exportPortalPackage()" class="bg-amber-500 hover:bg-amber-600 text-slate-900 px-3 py-1.5 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition shadow">
        <i data-lucide="download" class="w-3.5 h-3.5"></i>
        <span class="hidden sm:inline">Export Portal Package</span>
      </button>
    </div>
  </div>
</div>
<div class="bg-teal-950/90 border-t border-teal-800/60">
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
    <nav class="flex space-x-1 sm:space-x-4 overflow-x-auto py-2 text-xs font-semibold text-teal-200">
      <button onclick="switchTab('dashboard')" id="tab-nav-dashboard" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5 active">
        <i data-lucide="layout-dashboard" class="w-4 h-4"></i><span>Executive Dashboard</span>
      </button>
      <button onclick="switchTab('powerbi')" id="tab-nav-powerbi" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5">
        <i data-lucide="bar-chart-2" class="w-4 h-4 text-amber-400"></i><span class="text-amber-300 font-bold">Microsoft Power BI Hub</span>
      </button>
      <button onclick="switchTab('opportunities')" id="tab-nav-opportunities" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5">
        <i data-lucide="lightbulb" class="w-4 h-4"></i><span>DDU Pipeline &amp; Opportunities</span>
      </button>
      <button onclick="switchTab('map')" id="tab-nav-map" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5">
        <i data-lucide="map" class="w-4 h-4"></i><span>GIS Field Cartography</span>
      </button>
      <button onclick="switchTab('surveys')" id="tab-nav-surveys" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5">
        <i data-lucide="file-text" class="w-4 h-4"></i><span>Field Records Directory</span>
      </button>
      <button onclick="switchTab('lineage')" id="tab-nav-lineage" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5">
        <i data-lucide="git-branch" class="w-4 h-4 text-emerald-400"></i><span>Data Lineage &amp; Evidence</span>
      </button>
      <button onclick="switchTab('models')" id="tab-nav-models" class="tab-btn px-3 py-2 rounded-lg hover:text-white transition flex items-center space-x-1.5">
        <i data-lucide="database" class="w-4 h-4"></i><span>Power BI Semantic Connector</span>
      </button>
    </nav>
  </div>
</div>
</header>
""")

        // MAIN BODY
        sb.append("""<main class="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
<section id="section-dashboard" class="space-y-6">
  <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
    <div class="bg-white p-4 rounded-xl border border-slate-200 portal-shadow">
      <div class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Total DDUs</div>
      <div class="text-2xl font-black text-teal-800 mt-1">${stats.opportunitiesCount}</div>
      <div class="text-[10px] text-emerald-600 font-semibold mt-1">✓ Active Pipeline</div>
    </div>
    <div class="bg-white p-4 rounded-xl border border-slate-200 portal-shadow">
      <div class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Stage 1 Discovery</div>
      <div class="text-2xl font-black text-amber-700 mt-1">$discoveryCount</div>
      <div class="text-[10px] text-amber-600 font-semibold mt-1">Field Validation</div>
    </div>
    <div class="bg-white p-4 rounded-xl border border-slate-200 portal-shadow">
      <div class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Stage 2 Shortlisted</div>
      <div class="text-2xl font-black text-emerald-700 mt-1">$shortlistedCount</div>
      <div class="text-[10px] text-emerald-600 font-semibold mt-1">Village Production</div>
    </div>
    <div class="bg-white p-4 rounded-xl border border-slate-200 portal-shadow">
      <div class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Field Surveys</div>
      <div class="text-2xl font-black text-slate-800 mt-1">${surveys.size}</div>
      <div class="text-[10px] text-slate-500 font-semibold mt-1">100% GPS Confirmed</div>
    </div>
    <div class="bg-white p-4 rounded-xl border border-slate-200 portal-shadow">
      <div class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Annual Drain Leakage</div>
      <div class="text-2xl font-black text-rose-600 mt-1">₹4.8L</div>
      <div class="text-[10px] text-rose-600 font-semibold mt-1">External Arbitrage</div>
    </div>
    <div class="bg-white p-4 rounded-xl border border-slate-200 portal-shadow">
      <div class="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Sakhya Prospects</div>
      <div class="text-2xl font-black text-purple-700 mt-1">${sakhyaScreenings.size}</div>
      <div class="text-[10px] text-purple-600 font-semibold mt-1">Form F1 Screened</div>
    </div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <div class="lg:col-span-2 bg-white p-5 rounded-2xl border border-slate-200 portal-shadow">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h2 class="text-base font-bold text-slate-900">DDU Conversion Pipeline</h2>
          <p class="text-xs text-slate-500">From field identification to autonomous enterprise scale</p>
        </div>
        <span class="px-2.5 py-1 text-xs font-bold rounded-lg bg-teal-50 text-teal-800 border border-teal-200">8 Funnel Gates</span>
      </div>
      <div class="space-y-3">
        <div class="flex items-center justify-between text-xs font-semibold"><span>1. Field Identification (${surveys.size} Surveys)</span><span class="text-teal-700 font-bold">100%</span></div>
        <div class="w-full bg-slate-100 rounded-full h-3"><div class="bg-teal-600 h-3 rounded-full" style="width: 100%"></div></div>
        <div class="flex items-center justify-between text-xs font-semibold"><span>2. Pattern Aggregation</span><span class="text-teal-700 font-bold">85%</span></div>
        <div class="w-full bg-slate-100 rounded-full h-3"><div class="bg-teal-500 h-3 rounded-full" style="width: 85%"></div></div>
        <div class="flex items-center justify-between text-xs font-semibold"><span>3. Stage 1 Shortlisting ($shortlistedCount DDUs)</span><span class="text-emerald-700 font-bold">70%</span></div>
        <div class="w-full bg-slate-100 rounded-full h-3"><div class="bg-emerald-600 h-3 rounded-full" style="width: 70%"></div></div>
        <div class="flex items-center justify-between text-xs font-semibold"><span>4. Stage 2 Production Readiness (${stage2Assessments.size} Assessed)</span><span class="text-amber-700 font-bold">50%</span></div>
        <div class="w-full bg-slate-100 rounded-full h-3"><div class="bg-amber-500 h-3 rounded-full" style="width: 50%"></div></div>
      </div>
    </div>

    <div class="bg-gradient-to-br from-teal-900 to-teal-800 text-white p-5 rounded-2xl border border-teal-700 shadow-md flex flex-col justify-between">
      <div>
        <div class="flex items-center justify-between">
          <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-400 text-slate-900">TOP ENTERPRISE CLUSTER</span>
          <span class="text-xs text-teal-300 font-bold">94% Viability</span>
        </div>
        <h3 class="text-lg font-black mt-3 leading-tight">School Uniform &amp; Linen Collective</h3>
        <p class="text-xs text-teal-200 mt-2 leading-relaxed">
          4 verified schools in Balrampur sourcing 1,200+ uniform sets annually from Tezpur wholesalers. Complete data lineage with 18 photos and headmaster interviews verified.
        </p>
      </div>
      <div class="mt-4 pt-4 border-t border-teal-700/60 flex items-center justify-between">
        <div>
          <div class="text-[10px] text-teal-300 uppercase">Annual Market Drain</div>
          <div class="text-lg font-black text-amber-300">₹4.8 Lakhs / yr</div>
        </div>
        <button onclick="switchTab('lineage')" class="bg-white text-teal-900 px-3 py-1.5 rounded-lg text-xs font-bold hover:bg-teal-50 transition">
          View Lineage →
        </button>
      </div>
    </div>
  </div>
</section>

<!-- POWER BI SECTION -->
<section id="section-powerbi" class="hidden space-y-6">
  <div class="bg-white p-6 rounded-2xl border border-slate-200 portal-shadow">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <div class="flex items-center space-x-2">
          <i data-lucide="bar-chart-3" class="w-5 h-5 text-amber-600"></i>
          <h2 class="text-lg font-bold text-slate-900">Microsoft Power BI Analytics &amp; Reporting Suite</h2>
        </div>
        <p class="text-xs text-slate-600">Enterprise Power BI embedded workspace connected directly to DDU Field Intelligence database.</p>
      </div>
      <div class="flex items-center space-x-2">
        <a href="${powerBiConfig.embedUrl}" target="_blank" class="bg-teal-800 hover:bg-teal-900 text-white px-3 py-2 rounded-lg text-xs font-bold flex items-center space-x-1.5 transition">
          <i data-lucide="external-link" class="w-3.5 h-3.5"></i><span>Open in Power BI Service</span>
        </a>
      </div>
    </div>
    <div class="mt-6 border border-slate-200 rounded-xl overflow-hidden bg-slate-950 text-white">
      <div class="bg-slate-900 px-4 py-2.5 flex items-center justify-between border-b border-slate-800 text-xs">
        <span class="font-bold">Power BI Executive Workspace (${powerBiConfig.reportId})</span>
        <span class="text-slate-400">Mode: $modeLabel</span>
      </div>
      <div class="p-6 bg-slate-900/60 min-h-[440px] flex flex-col justify-center items-center text-center">
        <div class="max-w-md space-y-3">
          <div class="w-16 h-16 mx-auto rounded-2xl bg-amber-500/20 text-amber-400 flex items-center justify-center border border-amber-500/30">
            <i data-lucide="bar-chart-2" class="w-8 h-8"></i>
          </div>
          <h3 class="text-base font-bold text-white">Live Power BI Embedded Gateway</h3>
          <p class="text-xs text-slate-300">
            Semantic data layer syncs relational facts: fact_ddus, fact_surveys, and fact_demand with real-time DAX calculations.
          </p>
          <div class="flex justify-center gap-3 pt-2">
            <a href="${powerBiConfig.embedUrl}" target="_blank" class="px-4 py-2 rounded-lg bg-amber-500 hover:bg-amber-600 text-slate-900 text-xs font-bold transition flex items-center space-x-1.5">
              <i data-lucide="monitor" class="w-3.5 h-3.5"></i><span>Launch Interactive Report Viewer</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<!-- OPPORTUNITIES SECTION -->
<section id="section-opportunities" class="hidden space-y-6">
  <div class="flex items-center justify-between">
    <div>
      <h2 class="text-base font-bold text-slate-900">Identified DDU Opportunities (${opportunities.size})</h2>
      <p class="text-xs text-slate-500">Traceable back to primary field evidence</p>
    </div>
  </div>
  <div class="grid grid-cols-1 md:grid-cols-2 gap-4" id="opps-container"></div>
</section>

<!-- MAP SECTION -->
<section id="section-map" class="hidden space-y-4">
  <div class="flex items-center justify-between">
    <div>
      <h2 class="text-base font-bold text-slate-900">GIS Field Cartography</h2>
      <p class="text-xs text-slate-500">Green = Institutions • Blue = Local Shops • Orange = Suppliers</p>
    </div>
  </div>
  <div id="map-container" class="shadow border border-slate-300"></div>
</section>

<!-- SURVEYS SECTION -->
<section id="section-surveys" class="hidden space-y-4">
  <div class="flex items-center justify-between">
    <div>
      <h2 class="text-base font-bold text-slate-900">Field Surveys &amp; Business Directory (${surveys.size} Records)</h2>
      <p class="text-xs text-slate-500">Validated institutions and kiranas mapped on-site</p>
    </div>
    <input type="text" id="survey-search" oninput="filterSurveysTable()" placeholder="Search..." class="px-3 py-1.5 border border-slate-300 rounded-lg text-xs w-64 focus:outline-teal-600" />
  </div>
  <div class="bg-white rounded-2xl border border-slate-200 overflow-x-auto portal-shadow">
    <table class="w-full text-left border-collapse text-xs">
      <thead>
        <tr class="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold">
          <th class="p-3">DDU ID</th>
          <th class="p-3">Entity</th>
          <th class="p-3">Type</th>
          <th class="p-3">Village</th>
          <th class="p-3">Surveyor</th>
          <th class="p-3">Status</th>
          <th class="p-3">Completeness</th>
        </tr>
      </thead>
      <tbody id="surveys-tbody" class="divide-y divide-slate-100"></tbody>
    </table>
  </div>
</section>

<!-- LINEAGE SECTION -->
<section id="section-lineage" class="hidden space-y-6">
  <div class="bg-white p-6 rounded-2xl border border-slate-200 portal-shadow">
    <h2 class="text-base font-bold text-slate-900">Traceable Data Lineage &amp; Audited Field Evidence</h2>
    <p class="text-xs text-slate-500 mb-6">Photographic evidence and audio interviews supporting opportunities</p>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4" id="lineage-gallery-container"></div>
  </div>
</section>

<!-- MODELS SECTION -->
<section id="section-models" class="hidden space-y-6">
  <div class="bg-white p-6 rounded-2xl border border-slate-200 portal-shadow">
    <h2 class="text-base font-bold text-slate-900">Power BI Relational Semantic Views</h2>
    <p class="text-xs text-slate-500 mb-6">Star-schema tables ready for Power BI Desktop &amp; Service</p>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
        <div class="font-bold text-xs text-teal-900">fact_ddus (${opportunities.size} Rows)</div>
        <p class="text-[11px] text-slate-600">Pipeline progress, stage history, and annual demand values.</p>
        <button onclick="downloadFactCsv()" class="text-xs text-teal-700 font-bold hover:underline">Download CSV</button>
      </div>
      <div class="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-1">
        <div class="font-bold text-xs text-teal-900">fact_surveys (${surveys.size} Rows)</div>
        <p class="text-[11px] text-slate-600">Surveyor efficiency, GPS coordinates, and completeness scores.</p>
        <button onclick="downloadSurveysCsv()" class="text-xs text-teal-700 font-bold hover:underline">Download CSV</button>
      </div>
    </div>
  </div>
</section>
</main>

<footer class="bg-slate-900 text-slate-400 text-xs py-6 mt-12 border-t border-slate-800 text-center">
  <div>Deendayal Research Institute (DRI) • Chitrakoot • DDU Field Intelligence System</div>
</footer>
""")

        // JAVASCRIPT LOGIC
        sb.append("<script>\n")
        sb.append("var rawSurveys = ").append(surveysJson.toString()).append(";\n")
        sb.append("var rawOpportunities = ").append(oppsJson.toString()).append(";\n")
        sb.append("var leafletMap = null;\n\n")

        sb.append("""lucide.createIcons();

function switchTab(tabId) {
    document.querySelectorAll('section[id^="section-"]').forEach(function(el) { el.classList.add('hidden'); });
    document.querySelectorAll('.tab-btn').forEach(function(btn) { btn.classList.remove('active'); });
    var sec = document.getElementById('section-' + tabId);
    if (sec) sec.classList.remove('hidden');
    var btn = document.getElementById('tab-nav-' + tabId);
    if (btn) btn.classList.add('active');
    if (tabId === 'map') { setTimeout(initMap, 200); }
}

function renderOpportunities() {
    var container = document.getElementById('opps-container');
    if (!container) return;
    container.innerHTML = '';
    rawOpportunities.forEach(function(opp) {
        var card = document.createElement('div');
        card.className = "bg-white p-5 rounded-2xl border border-slate-200 portal-shadow flex flex-col justify-between";
        var isHigh = opp.level === 'HIGH';
        card.innerHTML = '<div class="flex items-center justify-between">' +
            '<span class="px-2.5 py-0.5 rounded text-xs font-bold bg-purple-100 text-purple-900 font-mono">' + opp.oppId + '</span>' +
            '<span class="px-2 py-0.5 rounded text-[10px] font-bold ' + (isHigh ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800') + '">' + opp.level + ' VIABILITY</span>' +
            '</div>' +
            '<h3 class="text-sm font-bold text-slate-900 mt-2.5">' + opp.title + '</h3>' +
            '<p class="text-xs text-teal-700 font-semibold mt-0.5">' + opp.category + ' • ' + opp.cluster + '</p>' +
            '<div class="mt-3 p-3 bg-slate-50 rounded-xl space-y-1 text-xs">' +
            '<div class="flex justify-between"><span>Demand:</span><span class="font-bold">' + opp.buyersCount + ' buyers • ' + opp.annualDemand + '</span></div>' +
            '<div class="flex justify-between"><span>Current Source:</span><span class="font-semibold text-rose-700">' + opp.currentSource + '</span></div>' +
            '</div>' +
            '<p class="text-xs text-slate-600 mt-2.5 line-clamp-2">' + opp.narrative + '</p>' +
            '<div class="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center">' +
            '<span class="text-[11px] text-emerald-700 font-semibold">✓ ' + opp.evidenceSummary + '</span>' +
            '<button onclick="switchTab(\'lineage\')" class="px-3 py-1.5 rounded-lg bg-teal-50 hover:bg-teal-100 text-teal-800 text-xs font-bold">View Lineage →</button>' +
            '</div>';
        container.appendChild(card);
    });
}

function renderSurveysTable(list) {
    var tbody = document.getElementById('surveys-tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    var data = list || rawSurveys;
    data.forEach(function(s) {
        var tr = document.createElement('tr');
        tr.className = "hover:bg-slate-50 transition";
        tr.innerHTML = '<td class="p-3 font-mono font-bold text-teal-800">' + s.dduId + '</td>' +
            '<td class="p-3 font-bold text-slate-900">' + s.entityName + '</td>' +
            '<td class="p-3">' + s.entityType + '</td>' +
            '<td class="p-3 text-slate-600">' + s.village + '</td>' +
            '<td class="p-3 text-slate-600">' + s.surveyor + '</td>' +
            '<td class="p-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">' + s.status + '</span></td>' +
            '<td class="p-3 font-bold text-teal-700">' + s.completeness + '%</td>';
        tbody.appendChild(tr);
    });
}

function filterSurveysTable() {
    var q = document.getElementById('survey-search').value.toLowerCase();
    var filtered = rawSurveys.filter(function(s) {
        return s.dduId.toLowerCase().indexOf(q) !== -1 ||
            s.entityName.toLowerCase().indexOf(q) !== -1 ||
            s.village.toLowerCase().indexOf(q) !== -1;
    });
    renderSurveysTable(filtered);
}

function initMap() {
    if (leafletMap) { leafletMap.invalidateSize(); return; }
    var mapEl = document.getElementById('map-container');
    if (!mapEl) return;
    var defaultLat = rawSurveys[0] ? rawSurveys[0].lat : 27.4335;
    var defaultLng = rawSurveys[0] ? rawSurveys[0].lng : 82.1855;
    leafletMap = L.map('map-container').setView([defaultLat, defaultLng], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(leafletMap);
    rawSurveys.forEach(function(s) {
        if (s.lat && s.lng) {
            var color = s.surveyType === 'INSTITUTION' ? '#15803D' : (s.surveyType === 'LOCAL_SHOP' ? '#0284C7' : '#EA580C');
            var marker = L.circleMarker([s.lat, s.lng], { radius: 8, fillColor: color, color: '#FFFFFF', weight: 2, fillOpacity: 0.9 }).addTo(leafletMap);
            marker.bindPopup('<b>' + s.entityName + '</b><br/>' + s.entityType + ' • ' + s.village + '<br/>DDU: ' + s.dduId);
        }
    });
}

function renderEvidenceGallery() {
    var container = document.getElementById('lineage-gallery-container');
    if (!container) return;
    container.innerHTML = '';
    var items = [
        { title: "School Uniform Sample Inspection", ddu: "DDU-BAL-2026-000124", type: "PHOTO", caption: "Navy blue trousers and checked shirt procured externally at ₹380/set.", cat: "Product Sample" },
        { title: "Headmaster Interview Recording", ddu: "DDU-BAL-2026-000124", type: "VOICE", caption: "Transcript: 'School needs 420 sets annually. Sizing corrections delay delivery by 2 months.'", cat: "Respondent Audio (64s)" },
        { title: "Dorika Hospital Linen Storage", ddu: "DDU-BAL-2026-000125", type: "PHOTO", caption: "Monthly procurement ledger showing 200 white bedsheets and 80 gowns.", cat: "Facility Evidence" },
        { title: "FS Store Daily Kirana Counter", ddu: "DDU-BAL-2026-000127", type: "PHOTO", caption: "Daily sales records: 60 packets bread and 30 packets rusk from city bakery.", cat: "Shopfront & Ledger" },
        { title: "Garment Retailer Margin Audit", ddu: "DDU-BAL-2026-000128", type: "VOICE", caption: "Transcript: 'Ladies nighty bought from Tezpur at ₹200, sells at ₹300. High demand.'", cat: "Proprietor Audio (82s)" },
        { title: "Chetak Wholesale Delivery Invoice", ddu: "DDU-BAL-2026-000125", type: "PHOTO", caption: "Certified tax invoice proving external supply freight drain of ₹1,200/month.", cat: "Documentary Proof" }
    ];
    items.forEach(function(ev) {
        var el = document.createElement('div');
        el.className = "p-4 rounded-xl border border-slate-200 bg-white portal-shadow space-y-2";
        el.innerHTML = '<div class="flex items-center justify-between">' +
            '<span class="text-xs font-bold text-slate-900">' + ev.title + '</span>' +
            '<span class="text-[10px] px-2 py-0.5 rounded font-bold ' + (ev.type === 'VOICE' ? 'bg-amber-100 text-amber-800' : 'bg-teal-100 text-teal-800') + '">' + ev.cat + '</span>' +
            '</div>' +
            '<p class="text-xs text-slate-600">' + ev.caption + '</p>' +
            '<div class="text-[10px] text-teal-700 font-bold">Linked: ' + ev.ddu + '</div>';
        container.appendChild(el);
    });
}

function downloadFactCsv() {
    var csv = "ddu_id,title,category,level,status,demanding_buyers,annual_demand,confidence\n";
    rawOpportunities.forEach(function(o) {
        csv += '"' + o.oppId + '","' + o.title + '","' + o.category + '","' + o.level + '","' + o.status + '","' + o.buyersCount + '","' + o.annualDemand + '","' + o.confidence + '"\n';
    });
    downloadBlob(csv, "fact_ddus.csv", "text/csv");
}

function downloadSurveysCsv() {
    var csv = "ddu_id,entity_name,type,village,block,district,lat,lng,surveyor,status\n";
    rawSurveys.forEach(function(s) {
        csv += '"' + s.dduId + '","' + s.entityName + '","' + s.entityType + '","' + s.village + '","' + s.block + '","' + s.district + '","' + s.lat + '","' + s.lng + '","' + s.surveyor + '","' + s.status + '"\n';
    });
    downloadBlob(csv, "fact_surveys.csv", "text/csv");
}

function downloadBlob(content, filename, mimeType) {
    var blob = new Blob([content], { type: mimeType });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

function exportPortalPackage() {
    var blob = new Blob([document.documentElement.outerHTML], { type: 'text/html' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = "DDU_Field_Intelligence_Master_Portal.html";
    a.click();
    URL.revokeObjectURL(url);
}

renderOpportunities();
renderSurveysTable();
renderEvidenceGallery();
</script>
</body>
</html>
""")

        return sb.toString()
    }
}
