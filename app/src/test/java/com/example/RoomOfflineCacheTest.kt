package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.EvidenceEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RecordStatus
import com.example.data.model.SurveyEntity
import com.example.data.model.SurveyType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomOfflineCacheTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomOfflineCacheAndPendingSync() = runBlocking {
        val surveyDao = database.surveyDao()

        val offlineSurvey = SurveyEntity(
            dduId = "DDU-TEST-001",
            surveyType = SurveyType.FIELD_OBSERVATION.name,
            entityName = "Rampur Tailoring Unit",
            entityType = "Tailoring Cluster",
            contactPerson = "Ramesh Kumar",
            contactNumber = "+91 9876543210",
            village = "Rampur Tola",
            tola = "Main Basti",
            block = "Balrampur",
            district = "Balrampur",
            gpsLatitude = 27.4300,
            gpsLongitude = 82.1880,
            gpsAccuracyMeters = 3.5f,
            gpsConfirmed = true,
            originalGpsLat = 27.4300,
            originalGpsLng = 82.1880,
            dateString = "24 Sep 2026",
            timeString = "10:30 AM",
            surveyorName = "Field Surveyor",
            status = RecordStatus.DRAFT.name,
            confidenceLevel = "HIGH",
            completenessScore = 85,
            isSynced = false,
            fieldIntelligenceSummary = "Offline captured tailoring cluster demand"
        )

        val product = ProductEntity(
            surveyDduId = "DDU-TEST-001",
            productName = "Uniform Sets",
            category = "Stitching & Garments",
            brand = "Local",
            unit = "Set",
            minQuantity = 50.0,
            maxQuantity = 100.0,
            buyingPrice = 350.0,
            buyingFrequency = "Quarterly",
            currentSupplier = "District Market",
            supplierContact = "+91 9876500000",
            currentSource = "Outside Village",
            potentialOpportunity = "Local DDU production"
        )

        // Save to Room offline cache
        surveyDao.insertSurvey(offlineSurvey)
        database.productDao().insertProducts(listOf(product))

        // Verify it was cached in Room
        val loaded = surveyDao.getSurveyById("DDU-TEST-001")
        assertEquals("Rampur Tailoring Unit", loaded?.entityName)
        assertFalse(loaded?.isSynced ?: true)

        // Verify pending sync count
        val pendingDirect = surveyDao.getPendingSurveysWithDetailsDirect()
        assertEquals(1, pendingDirect.size)
        assertEquals("DDU-TEST-001", pendingDirect.first().survey.dduId)
        assertEquals(1, pendingDirect.first().products.size)

        // Mark as synced to simulate successful sync to Firestore
        surveyDao.markAsSynced("DDU-TEST-001")

        val syncedSurvey = surveyDao.getSurveyById("DDU-TEST-001")
        assertTrue(syncedSurvey?.isSynced ?: false)

        val remainingPending = surveyDao.getPendingSurveysWithDetailsDirect()
        assertEquals(0, remainingPending.size)
    }
}
