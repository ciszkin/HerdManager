package by.ciszkin.herdmanager.presentation.error

import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.domain.error.ConnectionError
import by.ciszkin.herdmanager.domain.error.HttpError
import by.ciszkin.herdmanager.domain.error.ModelNotFoundError
import by.ciszkin.herdmanager.domain.error.OllamaUnavailableError
import by.ciszkin.herdmanager.domain.error.ParsingError
import by.ciszkin.herdmanager.domain.error.RegistryParseError
import by.ciszkin.herdmanager.domain.error.TimeoutError
import by.ciszkin.herdmanager.domain.error.UnexpectedError
import by.ciszkin.herdmanager.domain.error.ValidationError
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the error → message mapping and the en/be string catalogs:
 * every AppError subtype must resolve to a string resource, and both locales
 * must expose the same keys (missing translations silently fall back to the
 * default locale otherwise).
 */
class ErrorMessagesParityTest {

    @Test
    fun `every AppError subtype maps to a message resource`() {
        val errors: List<Pair<String, AppError>> = listOf(
            "TimeoutError" to TimeoutError(cause = null, operation = "op", timeoutMs = 1),
            "ConnectionError" to ConnectionError(cause = null, host = "h"),
            "HttpError 400" to HttpError(cause = null, statusCode = 400, endpoint = "e"),
            "HttpError 500" to HttpError(cause = null, statusCode = 500, endpoint = "e"),
            "HttpError 600" to HttpError(cause = null, statusCode = 600, endpoint = "e"),
            "OllamaUnavailableError" to OllamaUnavailableError(cause = null),
            "ModelNotFoundError" to ModelNotFoundError(cause = null, modelName = "m"),
            "ParsingError" to ParsingError(cause = null, contentType = "json", expectedFormat = "x"),
            "ValidationError" to ValidationError(cause = null, field = "f", constraint = "c"),
            "RegistryParseError" to RegistryParseError(cause = null),
            "UnexpectedError" to UnexpectedError(cause = null, context = "c")
        )

        errors.forEach { (label, error) ->
            val resource = error.toMessageResource()
            assertTrue(resource.key.isNotBlank(), "Expected $label to map to a named string resource")
        }
    }

    @Test
    fun `http errors map by status band`() {
        assertEquals("error_http_client_error", HttpError(null, 400, "e").toMessageResource().key)
        assertEquals("error_http_server_error", HttpError(null, 500, "e").toMessageResource().key)
        assertEquals("error_http_unknown", HttpError(null, 600, "e").toMessageResource().key)
    }

    @Test
    fun `en and be catalogs expose the same string keys`() {
        val enKeys = stringKeys("src/commonMain/composeResources/values/strings.xml")
        val beKeys = stringKeys("src/commonMain/composeResources/values-be/strings.xml")

        val missingInBe = enKeys - beKeys
        val extraInBe = beKeys - enKeys

        assertEquals(emptySet(), missingInBe, "Keys missing from values-be/strings.xml")
        assertEquals(emptySet(), extraInBe, "Keys present only in values-be/strings.xml")
    }

    private fun stringKeys(path: String): Set<String> {
        val file = File(path)
        assertTrue(file.exists(), "Resource file not found: $path (working dir: ${File(".").absolutePath})")
        val content = file.readText()
        return Regex("""<string name="([^"]+)"""").findAll(content)
            .map { it.groupValues[1] }
            .toSet()
    }
}