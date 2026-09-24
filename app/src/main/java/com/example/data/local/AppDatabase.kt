package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.EvidenceEntity
import com.example.data.model.OpportunityEntity
import com.example.data.model.ProductAliasEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SakhyaScreeningEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SurveyEntity
import com.example.data.model.VillageProductionAssessmentEntity

@Database(
    entities = [
        SurveyEntity::class,
        ProductEntity::class,
        EvidenceEntity::class,
        SupplierEntity::class,
        OpportunityEntity::class,
        ProductAliasEntity::class,
        SakhyaScreeningEntity::class,
        VillageProductionAssessmentEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun productDao(): ProductDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun supplierDao(): SupplierDao
    abstract fun opportunityDao(): OpportunityDao
    abstract fun productAliasDao(): ProductAliasDao
    abstract fun sakhyaScreeningDao(): SakhyaScreeningDao
    abstract fun villageProductionAssessmentDao(): VillageProductionAssessmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ddu_field_intel.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
