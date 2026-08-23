package by.ciszkin.herdmanager.data.connection

import by.ciszkin.herdmanager.domain.model.Settings
import by.ciszkin.herdmanager.domain.model.ThemeMode
import by.ciszkin.herdmanager.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Integration-style tests for ConnectionManager lifecycle.
 * Uses runBlocking (real time) because start()/stop() drive Dispatchers.IO
 * and the settings debounce runs with real-time delays.
 *
 * All clients are strict mockks whose calls fail immediately, so the
 * ConnectionMonitor's polling never touches the network and no background
 * threads leak between tests.
 */
class ConnectionManagerTest {

    private val settingsStateFlow = MutableStateFlow(defaultSettings())

    private val settingsRepository = mockk<SettingsRepository> {
        every { settingsFlow } returns settingsStateFlow.asStateFlow()
    }

    private var manager: ConnectionManager? = null

    /** Factory producing hermetic clients: polling calls throw, close is a no-op. */
    private fun mockClientFactory(): Pair<(String) -> HttpClient, MutableList<HttpClient>> {
        val created = mutableListOf<HttpClient>()
        val factory: (String) -> HttpClient = {
            mockk<HttpClient>().also { client ->
                every { client.close() } returns Unit
                created += client
            }
        }
        return factory to created
    }

    private fun defaultSettings() = Settings(
        serverUrl = "http://localhost:11434",
        refreshInterval = 5,
        pollingEnabled = false,
        language = "en",
        themeMode = ThemeMode.SYSTEM
    )

    @AfterTest
    fun tearDown() {
        manager?.stop()
        manager = null
    }

    @Test
    fun `getApiService suspends until init completes and returns the service`() {
        runBlocking {
            val (factory, _) = mockClientFactory()
            val connectionManager = ConnectionManager(settingsRepository, factory).also { manager = it }
            connectionManager.start()

            val service = withTimeout(5.seconds) {
                connectionManager.getApiService()
            }

            assertNotNull(service)
            assertEquals("http://localhost:11434", connectionManager.currentUrl)
            assertNotNull(connectionManager.getConnectionMonitorOrNull())
        }
    }

    @Test
    fun `getApiService called before start waits for initialization and returns the service`() {
        runBlocking {
            val (factory, _) = mockClientFactory()
            val connectionManager = ConnectionManager(settingsRepository, factory).also { manager = it }

            // Simulate the Desktop boot race: a fetch starts before the host's
            // start() effect has run, then start() happens shortly after.
            val fetched = async {
                withTimeout(5.seconds) { connectionManager.getApiService() }
            }
            delay(50)
            connectionManager.start()

            val service = fetched.await()
            assertNotNull(service)
            assertTrue(service === connectionManager.getApiService())
            assertEquals("http://localhost:11434", connectionManager.currentUrl)
        }
    }

    @Test
    fun `getApiService throws when start never called`() {
        runBlocking {
            val (factory, _) = mockClientFactory()
            val connectionManager = ConnectionManager(
                settingsRepository,
                factory,
                startWaitTimeoutMs = 100
            ).also { manager = it }

            assertFailsWith<IllegalStateException> {
                connectionManager.getApiService()
            }
        }
    }

    @Test
    fun `stop then start works and reinitializes`() {
        runBlocking {
            val (factory, _) = mockClientFactory()
            val connectionManager = ConnectionManager(settingsRepository, factory).also { manager = it }
            connectionManager.start()
            withTimeout(5.seconds) { connectionManager.getApiService() }

            connectionManager.stop()
            assertEquals(null, connectionManager.currentUrl)
            assertNull(connectionManager.getConnectionMonitorOrNull())

            connectionManager.start()
            val service = withTimeout(5.seconds) { connectionManager.getApiService() }
            assertNotNull(service)
            assertTrue(connectionManager.currentUrl != null)
        }
    }

    @Test
    fun `server URL change recreates the service`() {
        runBlocking {
            val (factory, _) = mockClientFactory()
            val connectionManager = ConnectionManager(settingsRepository, factory).also { manager = it }
            connectionManager.start()
            val first = withTimeout(5.seconds) { connectionManager.getApiService() }

            settingsStateFlow.value = settingsStateFlow.value.copy(serverUrl = "http://192.168.1.10:11434")

            // The debounce (500ms) runs on Dispatchers.IO with real time; poll on
            // the observable outcome (a new service instance) rather than on
            // currentUrl, which is set before the service is swapped.
            withTimeout(5.seconds) {
                while (connectionManager.getApiService() === first) {
                    delay(50)
                }
            }

            val second = withTimeout(5.seconds) { connectionManager.getApiService() }
            assertEquals("http://192.168.1.10:11434", connectionManager.currentUrl)
            assertTrue(first !== second, "Expected a new API service instance after URL change")
        }
    }

    @Test
    fun `recreating services closes the previous client`() {
        runBlocking {
            val (factory, created) = mockClientFactory()
            val connectionManager = ConnectionManager(settingsRepository, factory).also { manager = it }
            connectionManager.start()
            withTimeout(5.seconds) { connectionManager.getApiService() }

            settingsStateFlow.value = settingsStateFlow.value.copy(serverUrl = "http://192.168.1.10:11434")

            // Wait until the debounced recreation has actually created the
            // second client (currentUrl is set just before client creation,
            // so waiting on the URL alone can race ahead of it).
            withTimeout(5.seconds) {
                while (created.size < 2) {
                    delay(50)
                }
            }

            assertEquals(2, created.size)
            verify { created[0].close() }
            verify(exactly = 0) { created[1].close() }
        }
    }

    @Test
    fun `stop closes the current client`() {
        runBlocking {
            val (factory, created) = mockClientFactory()
            val connectionManager = ConnectionManager(settingsRepository, factory).also { manager = it }
            connectionManager.start()
            withTimeout(5.seconds) { connectionManager.getApiService() }

            connectionManager.stop()

            assertEquals(1, created.size)
            verify { created[0].close() }
        }
    }

    @Test
    fun `init failure surfaces through getApiService`() {
        runBlocking {
            val failingRepo = mockk<SettingsRepository> {
                every { settingsFlow } returns flow {
                    throw IllegalStateException("Cannot read settings")
                }
            }
            val (factory, _) = mockClientFactory()
            val connectionManager = ConnectionManager(failingRepo, factory).also { manager = it }
            connectionManager.start()

            assertFailsWith<IllegalStateException> {
                withTimeout(5.seconds) { connectionManager.getApiService() }
            }
        }
    }
}