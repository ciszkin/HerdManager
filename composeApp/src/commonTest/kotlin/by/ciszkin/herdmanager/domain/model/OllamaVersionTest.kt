package by.ciszkin.herdmanager.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class OllamaVersionTest {

    @Test
    fun `OllamaVersion stores version string correctly`() {
        val version = OllamaVersion(version = "0.1.23")
        assertEquals("0.1.23", version.version)
    }

    @Test
    fun `OllamaVersion handles version with build metadata`() {
        val version = OllamaVersion(version = "0.1.23-gcdc1e7a")
        assertEquals("0.1.23-gcdc1e7a", version.version)
    }
}
