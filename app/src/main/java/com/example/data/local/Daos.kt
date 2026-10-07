package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.EvidenceEntity
import com.example.data.model.OpportunityEntity
import com.example.data.model.ProductAliasEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SurveyEntity
import com.example.data.model.SurveyWithDetails
import com.example.data.model.VillageProductionAssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Transaction
    @Query("SELECT * FROM surveys ORDER BY createdTimestamp DESC")
    fun getAllSurveysWithDetails(): Flow<List<SurveyWithDetails>>

    @Transaction
    @Query("SELECT * FROM surveys WHERE dduId = :dduId LIMIT 1")
    fun getSurveyWithDetailsByDduId(dduId: String): Flow<SurveyWithDetails?>

    @Query("SELECT * FROM surveys WHERE dduId = :dduId LIMIT 1")
    suspend fun getSurveyById(dduId: String): SurveyEntity?

    @Query("SELECT * FROM surveys ORDER BY createdTimestamp DESC")
    fun getAllSurveys(): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE status = :status ORDER BY createdTimestamp DESC")
    fun getSurveysByStatus(status: String): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE isSynced = 0")
    fun getPendingSyncSurveys(): Flow<List<SurveyEntity>>

    @Transaction
    @Query("SELECT * FROM surveys WHERE isSynced = 0 ORDER BY createdTimestamp DESC")
    fun getPendingSyncSurveysWithDetails(): Flow<List<SurveyWithDetails>>

    @Transaction
    @Query("SELECT * FROM surveys WHERE isSynced = 0 ORDER BY createdTimestamp DESC")
    suspend fun getPendingSurveysWithDetailsDirect(): List<SurveyWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: SurveyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<SurveyEntity>)

    @Update
    suspend fun updateSurvey(survey: SurveyEntity)

    @Query("UPDATE surveys SET status = :status, validationRemarks = :remarks, validatedBy = :validator, validatedDate = :date, updatedTimestamp = :timestamp WHERE dduId = :dduId")
    suspend fun updateValidationStatus(dduId: String, status: String, remarks: String, validator: String, date: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE surveys SET isSynced = 1 WHERE dduId = :dduId")
    suspend fun markAsSynced(dduId: String)

    @Query("UPDATE surveys SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllAsSynced()

    @Delete
    suspend fun deleteSurvey(survey: SurveyEntity)

    @Query("SELECT COUNT(*) FROM surveys")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM surveys WHERE status = 'APPROVED'")
    fun getApprovedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM surveys WHERE isSynced = 0")
    fun getPendingSyncCount(): Flow<Int>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE surveyDduId = :dduId")
    fun getProductsForSurvey(dduId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE surveyDduId = :dduId")
    suspend fun getProductsForSurveyDirect(dduId: String): List<ProductEntity>

    @Query("DELETE FROM products WHERE surveyDduId = :dduId")
    suspend fun deleteProductsForSurvey(dduId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT DISTINCT productName FROM products ORDER BY productName ASC")
    fun getDistinctProductNames(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE productName LIKE '%' || :query || '%'")
    fun searchProductsByName(query: String): Flow<List<ProductEntity>>
}

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence ORDER BY timestamp DESC")
    fun getAllEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE surveyDduId = :dduId ORDER BY timestamp DESC")
    fun getEvidenceForSurvey(dduId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidence WHERE surveyDduId = :dduId ORDER BY timestamp DESC")
    suspend fun getEvidenceForSurveyDirect(dduId: String): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: EvidenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidenceList(evidenceList: List<EvidenceEntity>)

    @Delete
    suspend fun deleteEvidence(evidence: EvidenceEntity)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY supplierName ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)
}

@Dao
interface OpportunityDao {
    @Query("SELECT * FROM opportunities ORDER BY level ASC")
    fun getAllOpportunities(): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE oppId = :oppId LIMIT 1")
    fun getOpportunityById(oppId: String): Flow<OpportunityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunity(opportunity: OpportunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunities(opportunities: List<OpportunityEntity>)

    @Update
    suspend fun updateOpportunity(opportunity: OpportunityEntity)

    @Query("UPDATE opportunities SET status = :status, updatedTimestamp = :timestamp WHERE oppId = :oppId")
    suspend fun updateOpportunityStage(oppId: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE opportunities SET isShortlistedForStage2 = :isShortlisted, updatedTimestamp = :timestamp WHERE oppId = :oppId")
    suspend fun updateShortlistStatus(oppId: String, isShortlisted: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM opportunities WHERE isShortlistedForStage2 = 1")
    fun getShortlistedOpportunities(): Flow<List<OpportunityEntity>>
}

@Dao
interface ProductAliasDao {
    @Query("SELECT * FROM product_aliases")
    fun getAllAliases(): Flow<List<ProductAliasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: ProductAliasEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<ProductAliasEntity>)
}

@Dao
interface SakhyaScreeningDao {
    @Query("SELECT * FROM sakhya_screenings ORDER BY createdTimestamp DESC")
    fun getAllScreenings(): Flow<List<SakhyaScreeningEntity>>

    @Query("SELECT * FROM sakhya_screenings WHERE screeningId = :id LIMIT 1")
    fun getScreeningById(id: String): Flow<SakhyaScreeningEntity?>

    @Query("SELECT * FROM sakhya_screenings WHERE linkedDduId = :dduId ORDER BY createdTimestamp DESC")
    fun getScreeningsByDduId(dduId: String): Flow<List<SakhyaScreeningEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreening(screening: SakhyaScreeningEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenings(screenings: List<SakhyaScreeningEntity>)

    @Update
    suspend fun updateScreening(screening: SakhyaScreeningEntity)

    @Delete
    suspend fun deleteScreening(screening: SakhyaScreeningEntity)

    @Query("SELECT COUNT(*) FROM sakhya_screenings")
    suspend fun count(): Int
}

@Dao
interface VillageProductionAssessmentDao {
    @Query("SELECT * FROM stage2_village_assessments ORDER BY finalRankingOrder ASC, priorityLevel ASC, readinessScore DESC")
    fun getAllAssessments(): Flow<List<VillageProductionAssessmentEntity>>

    @Query("SELECT * FROM stage2_village_assessments WHERE assessmentId = :id LIMIT 1")
    fun getAssessmentById(id: String): Flow<VillageProductionAssessmentEntity?>

    @Query("SELECT * FROM stage2_village_assessments WHERE stage1OppId = :oppId LIMIT 1")
    fun getAssessmentByOppId(oppId: String): Flow<VillageProductionAssessmentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: VillageProductionAssessmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<VillageProductionAssessmentEntity>)

    @Update
    suspend fun updateAssessment(assessment: VillageProductionAssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: VillageProductionAssessmentEntity)

    @Query("DELETE FROM stage2_village_assessments WHERE assessmentId = :id")
    suspend fun deleteAssessmentById(id: String)

    @Query("SELECT COUNT(*) FROM stage2_village_assessments")
    suspend fun count(): Int
}
