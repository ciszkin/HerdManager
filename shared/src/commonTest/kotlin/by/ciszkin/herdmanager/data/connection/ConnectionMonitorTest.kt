package by.ciszkin.herdmanager.data.connection

import by.ciszkin.herdmanager.data.api.OllamaApiService
import by.ciszkin.herdmanager.domain.model.ConnectionState
import by.ciszkin.herdmanager.domain.model.RunningModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionMonitorTest {

    private var scope: CoroutineScope? = null
    private lateinit var apiService: OllamaApiService

    private val runningModels = listOf(
        RunningModel(
            name = "llama2",
            model = "llama2:latest",
            size = 3_800_000_000,
            digest = "abc123"
        )
    )

    @AfterTest
    fun tearDown() {
        scope?.cancel()
        scope = null
    }

    @Test
    fun `initial state is Unknown`() = runTest {
        apiService = mockk()

        val monitor = ConnectionMonitor(apiService)

        assertIs<ConnectionState.Unknown>(monitor.state.value)
    }

    @Test
    fun `reflects Running models with count`() = runTest {
        apiService = mockk {
            coEvery { getRunningModels() } returns runningModels
        }
        val monitor = ConnectionMonitor(apiService)
        monitor.start(backgroundScope)
        runCurrent()

        val state = monitor.state.value
        assertIs<ConnectionState.Running>(state)
        assertEquals(1, state.count)

        monitor.stop()
    }

    @Test
    fun `reflects Idle when no models are running`() = runTest {
        apiService = mockk {
            coEvery { getRunningModels() } returns emptyList()
        }
        val monitor = ConnectionMonitor(apiService)
        monitor.start(backgroundScope)
        runCurrent()

        assertIs<ConnectionState.Idle>(monitor.state.value)

        monitor.stop()
    }

    @Test
    fun `reflects Disconnected on failure`() = runTest {
        apiService = mockk {
            coEvery { getRunningModels() } throws IllegalStateException("connection refused")
        }
        val monitor = ConnectionMonitor(apiService)
        monitor.start(backgroundScope)
        runCurrent()

        val state = monitor.state.value
        assertIs<ConnectionState.Disconnected>(state)
        assertEquals("connection refused", state.reason)

        monitor.stop()
    }

    @Test
    fun `polls every interval`() = runTest {
        apiService = mockk {
            coEvery { getRunningModels() } returns emptyList()
        }
        val monitor = ConnectionMonitor(apiService)
        monitor.start(backgroundScope)

        // First check runs immediately
        runCurrent()
        assertIs<ConnectionState.Idle>(monitor.state.value)

        // No check should have happened again after only 14s of the 15s interval
        advanceTimeBy(14_000)
        runCurrent()
        coVerify(exactly = 1) { apiService.getRunningModels() }

        // Crossing the interval triggers the second poll
        advanceTimeBy(1_000)
        runCurrent()
        coVerify(exactly = 2) { apiService.getRunningModels() }

        monitor.stop()
    }

    @Test
    fun `stop cancels polling`() = runTest {
        apiService = mockk {
            coEvery { getRunningModels() } returns emptyList()
        }
        val monitor = ConnectionMonitor(apiService)
        monitor.start(backgroundScope)
        runCurrent()

        monitor.stop()

        // No further polls after stopping
        advanceTimeBy(30_000)
        runCurrent()
        coVerify(exactly = 1) { apiService.getRunningModels() }
    }
}