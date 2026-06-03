package by.ciszkin.herdmanager.data.scraping

import by.ciszkin.herdmanager.domain.model.RegistryModel
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object OllamaLibraryScraper : KoinComponent {
    private const val BASE_URL = "https://ollama.com/search"

    private val httpClient: HttpClient by inject(named("scraper"))

    suspend fun fetchModels(query: String, page: Int): Result<List<RegistryModel>> = kotlin.runCatching {
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

    private fun parseModelsFromHtml(doc: org.jsoup.nodes.Document): List<RegistryModel> {
        val modelElements = doc.select("li[x-test-model]")
        val models = modelElements.mapNotNull { modelElement ->
            try {
                val linkElement = modelElement.selectFirst("a[href^=/]") ?: return@mapNotNull null
                val name = linkElement.attr("href").removePrefix("/library/").removePrefix("/i/")

                val titleElement = modelElement.selectFirst("span[x-test-search-response-title]")
                    ?: return@mapNotNull null
                val displayName = titleElement.text()

                val descriptionElement = modelElement.selectFirst("div > p")
                    ?: return@mapNotNull null
                val description = descriptionElement.text()

                val pullCountText = modelElement.selectFirst("span[x-test-pull-count]")?.text() ?: "0"
                val pullCount = parsePullCount(pullCountText)

                val updatedElement = modelElement.selectFirst("span[x-test-updated]")
                val updatedAt = updatedElement?.text() ?: ""

                val sizeTags = modelElement.select("span[x-test-size]").map { it.text() }
                val capabilities = modelElement.select("span[x-test-capability]")
                    .mapNotNull { it.text() }
                    .toList()

                RegistryModel(
                    id = name,
                    name = displayName,
                    description = description,
                    pullCount = pullCount,
                    tags = sizeTags,
                    capabilities = capabilities,
                    updatedAt = updatedAt.takeIf { it.isNotEmpty() }
                )
            } catch (_: Exception) {
                null
            }
        }
        return models
    }

    private fun parsePullCount(text: String): Long {
        val normalized = text.uppercase().replace(" ", "").replace(",", "")
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
