package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Sign In")
    object Home : Screen("home", "Home")
    object SurveyWizard : Screen("survey_wizard", "New Survey")
    object Records : Screen("records", "Records")
    object Map : Screen("map", "Field Map")
    object Insights : Screen("insights", "Insights")
    object RecordDetail : Screen("record_detail/{dduId}", "Record Detail") {
        fun createRoute(dduId: String) = "record_detail/$dduId"
    }
    object OpportunityDetail : Screen("opportunity_detail/{oppId}", "Opportunity Detail") {
        fun createRoute(oppId: String) = "opportunity_detail/$oppId"
    }
    object QuickObservation : Screen("quick_observation", "Quick Observation")
    object EvidenceLibrary : Screen("evidence_library", "Evidence Library")
    object ExportReports : Screen("export_reports", "Export & Reports")
    object OpportunityGraph : Screen("opportunity_graph", "Network Graph")
    object PhotoCapture : Screen("photo_capture?dduId={dduId}&oppId={oppId}", "Field Photo Verification") {
        fun createRoute(dduId: String? = null, oppId: String? = null): String {
            val params = mutableListOf<String>()
            if (!dduId.isNullOrBlank()) params.add("dduId=$dduId")
            if (!oppId.isNullOrBlank()) params.add("oppId=$oppId")
            return if (params.isEmpty()) "photo_capture" else "photo_capture?${params.joinToString("&")}"
        }
    }
    object SakhyaScreeningList : Screen("sakhya_list", "Sakhya Prospects")
    object SakhyaScreeningForm : Screen("sakhya_form?screeningId={screeningId}&dduId={dduId}", "Sakhya Screening Form (Form F1)") {
        fun createRoute(screeningId: String? = null, dduId: String? = null): String {
            val params = mutableListOf<String>()
            if (!screeningId.isNullOrBlank()) params.add("screeningId=$screeningId")
            if (!dduId.isNullOrBlank()) params.add("dduId=$dduId")
            return if (params.isEmpty()) "sakhya_form" else "sakhya_form?${params.joinToString("&")}"
        }
    }
    object SakhyaDetail : Screen("sakhya_detail/{screeningId}", "Sakhya Prospect Assessment") {
        fun createRoute(screeningId: String) = "sakhya_detail/$screeningId"
    }
    object Stage2AssessmentForm : Screen("stage2_form?assessmentId={assessmentId}&oppId={oppId}", "Stage 2 · Village Production Assessment") {
        fun createRoute(assessmentId: String? = null, oppId: String? = null): String {
            val params = mutableListOf<String>()
            if (!assessmentId.isNullOrBlank()) params.add("assessmentId=$assessmentId")
            if (!oppId.isNullOrBlank()) params.add("oppId=$oppId")
            return if (params.isEmpty()) "stage2_form" else "stage2_form?${params.joinToString("&")}"
        }
    }
    object Stage2DduSelection : Screen("stage2_ddu_selection", "Stage 2 · Final Product List for DDU Selection")
    object Analytics : Screen("analytics", "Analytics")
    object WebPortal : Screen("web_portal", "Master Web Portal")
}

