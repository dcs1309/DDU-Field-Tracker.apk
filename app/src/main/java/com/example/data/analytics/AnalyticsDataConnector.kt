package com.example.data.analytics

import com.example.data.model.OpportunityEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.VillageProductionAssessmentEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data Connector that prepares normalized relational Fact & Dimension tables
 * consumable by Microsoft Power BI Desktop, Power BI Service, and REST APIs.
 */
object AnalyticsDataConnector {

    /**
     * Generates a complete Power BI dataset payload formatted as a relational schema
     * with fact and dimension tables.
     */
    fun generatePowerBiDatasetJson(
        surveys: List<SurveyWithDetails>,
        opportunities: List<OpportunityEntity>,
        stage2Assessments: List<VillageProductionAssessmentEntity>,
        sakhyaScreenings: List<SakhyaScreeningEntity>
    ): String {
        val root = JSONObject()
        root.put("datasetName", "DDU_Field_Intelligence_Semantic_Model")
        root.put("version", "3.0")
        root.put("generatedAt", System.currentTimeMillis())

        // 1. fact_ddus
        val factDdus = JSONArray()
        opportunities.forEach { opp ->
            val obj = JSONObject()
            obj.put("ddu_id", opp.oppId)
            obj.put("title", opp.title)
            obj.put("category", opp.category)
            obj.put("status", opp.status)
            obj.put("stage", if (opp.isShortlistedForStage2) "STAGE_2_VILLAGE" else "STAGE_1_OPPORTUNITY")
            obj.put("confidence", opp.confidence)
            obj.put("demanding_entities_count", opp.demandingEntitiesCount)
            obj.put("annual_demand_estimate", opp.estimatedAnnualDemand)
            obj.put("buying_pattern", opp.buyingPattern)
            obj.put("current_source", opp.currentSource)
            obj.put("local_supply_status", opp.localSupplyStatus)
            obj.put("cluster", opp.geographicCluster)
            obj.put("is_stage2_selected", opp.isShortlistedForStage2)
            factDdus.put(obj)
        }
        root.put("fact_ddus", factDdus)

        // 2. fact_surveys
        val factSurveys = JSONArray()
        surveys.forEach { item ->
            val s = item.survey
            val obj = JSONObject()
            obj.put("survey_id", s.dduId)
            obj.put("entity_name", s.entityName)
            obj.put("survey_type", s.surveyType)
            obj.put("entity_type", s.entityType)
            obj.put("village_id", s.village)
            obj.put("block_id", s.block)
            obj.put("district_id", s.district)
            obj.put("latitude", s.gpsLatitude)
            obj.put("longitude", s.gpsLongitude)
            obj.put("accuracy_meters", s.gpsAccuracyMeters)
            obj.put("status", s.status)
            obj.put("confidence_level", s.confidenceLevel)
            obj.put("completeness_score", s.completenessScore)
            obj.put("survey_date", s.dateString)
            obj.put("surveyor_name", s.surveyorName)
            obj.put("is_synced", s.isSynced)
            factSurveys.put(obj)
        }
        root.put("fact_surveys", factSurveys)

        // 3. fact_demand
        val factDemand = JSONArray()
        surveys.forEach { item ->
            val s = item.survey
            item.products.forEach { prod ->
                val obj = JSONObject()
                obj.put("demand_id", "DEM_${s.dduId}_${prod.id}")
                obj.put("survey_ddu_id", s.dduId)
                obj.put("product_name", prod.productName)
                obj.put("min_quantity", prod.minQuantity)
                obj.put("max_quantity", prod.maxQuantity)
                obj.put("unit", prod.unit)
                obj.put("unit_price", prod.buyingPrice)
                obj.put("total_monthly_value", prod.minQuantity * prod.buyingPrice)
                obj.put("purchase_frequency", prod.buyingFrequency)
                obj.put("existing_supplier", prod.currentSupplier)
                obj.put("is_external_source", prod.currentSource.contains("Outside", ignoreCase = true))
                factDemand.put(obj)
            }
        }
        root.put("fact_demand", factDemand)

        // 4. fact_stage_history
        val factStageHistory = JSONArray()
        stage2Assessments.forEach { assessment ->
            val obj = JSONObject()
            obj.put("history_id", "HIST_${assessment.assessmentId}")
            obj.put("ddu_id", assessment.stage1OppId)
            obj.put("village", assessment.vatika)
            obj.put("stage", "STAGE_2_VILLAGE_PRODUCTION")
            obj.put("status", assessment.finalSelectionStatus)
            obj.put("readiness_score", assessment.readinessScore)
            obj.put("priority_level", assessment.priorityLevel)
            obj.put("sample_available", assessment.isSampleAvailable)
            factStageHistory.put(obj)
        }
        root.put("fact_stage_history", factStageHistory)

        // 5. dim_products
        val dimProducts = JSONArray()
        val distinctProducts = surveys.flatMap { it.products }.map { it.productName.trim() }.distinct()
        distinctProducts.forEachIndexed { idx, name ->
            val obj = JSONObject()
            obj.put("product_id", "PROD_${idx + 1}")
            obj.put("product_name", name)
            obj.put("category", categorizeProduct(name))
            dimProducts.put(obj)
        }
        root.put("dim_products", dimProducts)

        // 6. dim_locations
        val dimLocations = JSONArray()
        val distinctVillages = surveys.map { "${it.survey.village}|${it.survey.block}|${it.survey.district}" }.distinct()
        distinctVillages.forEachIndexed { idx, loc ->
            val parts = loc.split("|")
            val obj = JSONObject()
            obj.put("location_id", "LOC_${idx + 1}")
            obj.put("village", parts.getOrElse(0) { "" })
            obj.put("block", parts.getOrElse(1) { "" })
            obj.put("district", parts.getOrElse(2) { "" })
            obj.put("state", "Assam")
            dimLocations.put(obj)
        }
        root.put("dim_locations", dimLocations)

        // 7. dim_sakhyas
        val dimSakhyas = JSONArray()
        sakhyaScreenings.forEach { sakhya ->
            val obj = JSONObject()
            obj.put("sakhya_id", sakhya.screeningId)
            obj.put("name", sakhya.entrepreneurName)
            obj.put("enterprise_trade", sakhya.productAndValueChain)
            obj.put("screening_score", sakhya.readinessScore)
            obj.put("recommendation", sakhya.status)
            dimSakhyas.put(obj)
        }
        root.put("dim_sakhyas", dimSakhyas)

        return root.toString(2)
    }

    /**
     * Generates clean CSV tables suitable for importing into Power BI Desktop
     */
    fun generateFactDdusCsv(opportunities: List<OpportunityEntity>): String {
        val sb = StringBuilder()
        sb.append("ddu_id,title,category,level,status,stage,demanding_entities,annual_demand,current_source,confidence\n")
        opportunities.forEach { opp ->
            val stageName = if (opp.isShortlistedForStage2) "STAGE_2_VILLAGE" else "STAGE_1_OPPORTUNITY"
            sb.append("\"${opp.oppId}\",\"${opp.title}\",\"${opp.category}\",\"${opp.level}\",\"${opp.status}\",\"$stageName\",${opp.demandingEntitiesCount},\"${opp.estimatedAnnualDemand}\",\"${opp.currentSource}\",\"${opp.confidence}\"\n")
        }
        return sb.toString()
    }

    fun generateFactDemandCsv(surveys: List<SurveyWithDetails>): String {
        val sb = StringBuilder()
        sb.append("demand_id,survey_ddu_id,entity_name,village,product_name,min_quantity,unit,unit_price,total_value,frequency,supplier,is_external\n")
        surveys.forEach { item ->
            val s = item.survey
            item.products.forEach { p ->
                val totalVal = p.minQuantity * p.buyingPrice
                val isExt = p.currentSource.contains("Outside", ignoreCase = true)
                sb.append("\"DEM_${s.dduId}_${p.id}\",\"${s.dduId}\",\"${s.entityName}\",\"${s.village}\",\"${p.productName}\",${p.minQuantity},\"${p.unit}\",${p.buyingPrice},$totalVal,\"${p.buyingFrequency}\",\"${p.currentSupplier}\",$isExt\n")
            }
        }
        return sb.toString()
    }

    private fun categorizeProduct(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("phenyl") || n.contains("cleaner") || n.contains("soap") || n.contains("wash") || n.contains("detergent") -> "Cleaning Products"
            n.contains("uniform") || n.contains("gown") || n.contains("bedsheet") || n.contains("sheet") || n.contains("pillow") || n.contains("fabric") -> "Textiles & Linen"
            n.contains("pickle") || n.contains("biscuit") || n.contains("nimki") || n.contains("pitha") || n.contains("snack") || n.contains("spice") -> "Food Products"
            n.contains("agarbatti") || n.contains("incense") || n.contains("candle") -> "Agarbatti & Aromatics"
            n.contains("poultry") || n.contains("duck") || n.contains("egg") || n.contains("goat") || n.contains("mushroom") -> "Agriculture & Livestock"
            n.contains("mekhela") || n.contains("gamosa") || n.contains("handloom") || n.contains("chador") -> "Handloom & Craft"
            else -> "Other Field Opportunity"
        }
    }
}
