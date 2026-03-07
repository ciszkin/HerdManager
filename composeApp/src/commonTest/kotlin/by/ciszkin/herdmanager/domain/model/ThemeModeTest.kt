package by.ciszkin.herdmanager.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {

    @Test
    fun `fromValue returns correct ThemeMode`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromValue("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromValue("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromValue("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue("invalid"))
    }

    @Test
    fun `value returns correct string`() {
        assertEquals("light", ThemeMode.LIGHT.value)
        assertEquals("dark", ThemeMode.DARK.value)
        assertEquals("system", ThemeMode.SYSTEM.value)
    }
}
