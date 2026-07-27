package by.ciszkin.herdmanager.data.scraping

import by.ciszkin.herdmanager.domain.model.RegistryModel
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object OllamaLibraryScraper : KoinComponent {
    private const val BASE_URL = "https://ollama.com/search"

    private val httpClient: HttpClient by inject(named("scraper"))

    suspend fun fetchModels(query: String, page: Int): Result<List<RegistryModel>> = runCatching {
        val url = when {
            query.isEmpty() && page == 1 -> BASE_URL
            query.isEmpty() -> "$BASE_URL?page=$page"
            page == 1 -> "$BASE_URL?q=$query"
            else -> "$BASE_URL?q=$query&page=$page"
        }

        val html: String = withContext(Dispatchers.IO) {
            httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
                    append("Accept", "*/*")
                    append("hx-request", "true")
                    append("hx-current-url", "https://ollama.com/search")
                }
            }.bodyAsText()
        }

        val doc = Jsoup.parse(html)

        parseModelsFromHtml(doc)
    }

    /**
     * Parses model cards from the ollama.com search page.
     *
     * ollama.com removed the `x-test-*` test hooks, so we anchor on the
     * `group w-full` card link inside each `<li>` and read the surrounding
     * markup: the `<h2>` for the name, `p.max-w-lg` for the description, and
     * the `p.my-1` stats row for pull count and last-updated time. The badges
     * in `div.flex-wrap` are color-coded: blue (`text-blue-600`) = size tags,
     * indigo (`text-indigo-600`) = capabilities. Other badges (e.g. the cyan
     * "cloud" availability flag) are ignored.
     *
     * The same card markup wraps both official library models
     * (`/library/<name>`) and community models (`/<user>/<model>`), so we match
     * on the card anchor rather than the `/library/` href — otherwise a search
     * like "bonsai" drops every community result.
     */
    internal fun parseModelsFromHtml(doc: Document): List<RegistryModel> {
        val modelElements = doc.select("li:has(a.group.w-full)")
        return modelElements.mapNotNull { modelElement ->
            try {
                val linkElement = modelElement.selectFirst("a.group.w-full")
                    ?: return@mapNotNull null
                val href = linkElement.attr("href")
                val id = when {
                    href.startsWith("/library/") -> href.removePrefix("/library/")
                    href.startsWith("/i/") -> href.removePrefix("/i/")
                    else -> href.removePrefix("/")
                }

                val displayName = modelElement.selectFirst("h2")?.text()?.takeIf { it.isNotEmpty() } ?: id

                val description = modelElement.selectFirst("p.max-w-lg")?.text() ?: ""

                val sizeTags = modelElement.select("div.flex-wrap > span.text-blue-600")
                    .map { it.text() }
                    .filter { it.isNotEmpty() }

                val capabilities = modelElement.select("div.flex-wrap > span.text-indigo-600")
                    .map { it.text() }
                    .filter { it.isNotEmpty() }

                var pullCount = 0L
                var updatedAt: String? = null
                modelElement.select("p.my-1 > span").forEach { stat ->
                    val text = stat.text()
                    when {
                        "Pulls" in text -> pullCount = parsePullCount(text)
                        "Updated" in text -> updatedAt = stat.attr("title").ifBlank { null } ?: text
                    }
                }

                RegistryModel(
                    id = id,
                    name = displayName,
                    description = description,
                    pullCount = pullCount,
                    tags = sizeTags,
                    capabilities = capabilities,
                    updatedAt = updatedAt
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    internal fun parsePullCount(text: String): Long {
        val token = Regex("""[\d,]+(?:\.\d+)?\s*[KkMm]?""").find(text)?.value ?: return 0L
        val normalized = token.uppercase().replace(" ", "").replace(",", "")
        return when {
            normalized.endsWith("M") -> {
                normalized.removeSuffix("M").toDoubleOrNull()?.times(1_000_000)?.toLong() ?: 0L
            }

            normalized.endsWith("K") -> {
                normalized.removeSuffix("K").toDoubleOrNull()?.times(1_000)?.toLong() ?: 0L
            }

            else -> {
                normalized.toLongOrNull() ?: 0L
            }
        }
    }
}
