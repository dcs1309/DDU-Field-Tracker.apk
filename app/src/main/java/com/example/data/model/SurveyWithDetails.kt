package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SurveyWithDetails(
    @Embedded val survey: SurveyEntity,
    @Relation(
        parentColumn = "dduId",
        entityColumn = "surveyDduId"
    )
    val products: List<ProductEntity>,
    @Relation(
        parentColumn = "dduId",
        entityColumn = "surveyDduId"
    )
    val evidenceList: List<EvidenceEntity>
)
