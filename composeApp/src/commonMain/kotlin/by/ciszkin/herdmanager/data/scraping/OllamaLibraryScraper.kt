package by.ciszkin.herdmanager.data.scraping

import by.ciszkin.herdmanager.di.AppModule
import by.ciszkin.herdmanager.domain.model.RegistryModel
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

object OllamaLibraryScraper {
    private const val BASE_URL = "https://ollama.com/search"

    private val httpClient get() = AppModule.scraperHttpClient

    fun fetchModels(query: String, page: Int): Result<List<RegistryModel>> = runCatching {
        val url = when {
            query.isEmpty() && page == 1 -> BASE_URL
            query.isEmpty() -> "$BASE_URL?page=$page"
            page == 1 -> "$BASE_URL?q=$query"
            else -> "$BASE_URL?q=$query&page=$page"
        }

        val html: String = runBlocking {
            httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0")
                }
            }.bodyAsText()
        }

        val doc = Jsoup.parse(html)

        parseModelsFromHtml(doc)
    }

    private fun parseModelsFromHtml(doc: org.jsoup.nodes.Document): List<RegistryModel> {
        return doc.select("li[x-test-model]").mapNotNull { modelElement ->
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
