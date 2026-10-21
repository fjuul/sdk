package com.fjuul.sdk.activitysources.entities.internal.healthconnect

import android.os.Build
import android.os.RemoteException
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.response.ChangesResponse
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService
import com.fjuul.sdk.core.entities.InMemoryStorage
import com.fjuul.sdk.core.entities.Keystore
import com.fjuul.sdk.core.entities.SigningKey
import com.fjuul.sdk.core.entities.UserCredentials
import com.fjuul.sdk.core.exceptions.ApiExceptions
import com.fjuul.sdk.test.http.TestApiClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class HealthConnectDataManagerTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var storage: InMemoryStorage
    private lateinit var dataManager: HealthConnectDataManager

    private val options = HealthConnectSyncOptions(setOf(FitnessMetricsType.INTRADAY_HEART_RATE))
    private val lowerDateBoundary = Date.from(Instant.now().minus(Duration.ofDays(1)))
    private var foregroundOnlyRecordTypes = emptySet<Any>()

    // Health Connect reports the stored token as expired, which forces a full sync.
    private val client = object : HealthConnectClient by mock(HealthConnectClient::class.java) {
        override suspend fun getChanges(changesToken: String) =
            ChangesResponse(emptyList(), "", false, changesToken == EXPIRED_TOKEN)

        override suspend fun getChangesToken(request: ChangesTokenRequest): String {
            if (request.recordTypes.any { it in foregroundOnlyRecordTypes }) {
                throw RemoteException("app must be in foreground to read the following data types")
            }
            return FRESH_TOKEN
        }

        override suspend fun aggregateGroupByDuration(
            request: AggregateGroupByDurationRequest
        ): List<AggregationResultGroupedByDuration> {
            val start = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(Duration.ofMinutes(5))
            val result = AggregationResult(
                mapOf(HeartRateRecord.BPM_AVG.metricKey to 80L),
                emptyMap(),
                emptySet()
            )
            return listOf(
                AggregationResultGroupedByDuration(result, start, start.plusSeconds(60), ZoneOffset.UTC)
            )
        }
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val keystore = Keystore(InMemoryStorage())
        keystore.setKey(SigningKey("key-id", "secret", Date.from(Instant.now().plus(Duration.ofHours(2)))))
        val apiClient = TestApiClient.Builder(mockWebServer)
            .setKeystore(keystore)
            .setUserCredentials(UserCredentials("USER_TOKEN", "USER_SECRET"))
            .build()
        storage = InMemoryStorage()
        storage.set(HealthConnectDataManager.HEART_RATE_CHANGES_TOKEN, EXPIRED_TOKEN)
        dataManager = HealthConnectDataManager(client, ActivitySourcesService(apiClient), storage)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun syncIntraday_expiredTokenAndUploadFails_throwsToCallerAndKeepsToken() {
        repeat(3) {
            mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        }

        assertThrows(ApiExceptions.InternalClientException::class.java) {
            runBlocking { dataManager.syncIntraday(options, lowerDateBoundary) }
        }
        assertEquals(EXPIRED_TOKEN, storage.get(HealthConnectDataManager.HEART_RATE_CHANGES_TOKEN))
    }

    @Test
    fun syncIntraday_expiredTokenAndUploadSucceeds_persistsFreshToken() {
        repeat(3) { mockWebServer.enqueue(MockResponse().setResponseCode(200)) }

        runBlocking { dataManager.syncIntraday(options, lowerDateBoundary) }

        assertEquals(FRESH_TOKEN, storage.get(HealthConnectDataManager.HEART_RATE_CHANGES_TOKEN))
    }

    @Test
    fun syncIntraday_changesTokenReadDeniedInBackground_throwsToCaller() {
        foregroundOnlyRecordTypes = setOf(TotalCaloriesBurnedRecord::class)
        repeat(10) { mockWebServer.enqueue(MockResponse().setResponseCode(200)) }
        val caloriesOptions = HealthConnectSyncOptions(setOf(FitnessMetricsType.INTRADAY_CALORIES))

        assertThrows(RemoteException::class.java) {
            runBlocking { dataManager.syncIntraday(caloriesOptions, lowerDateBoundary) }
        }
        assertEquals(null, storage.get(HealthConnectDataManager.TOTAL_CALORIES_CHANGES_TOKEN))
    }

    private companion object {
        const val EXPIRED_TOKEN = "expired-token"
        const val FRESH_TOKEN = "fresh-token"
    }
}
