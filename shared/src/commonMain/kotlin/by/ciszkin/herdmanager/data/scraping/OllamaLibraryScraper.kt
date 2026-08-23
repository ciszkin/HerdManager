package by.ciszkin.herdmanager.data.scraping

import by.ciszkin.herdmanager.domain.error.RegistryParseException
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.domain.model.RegistrySort
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

object OllamaLibraryScraper : KoinComponent {
    private const val BASE_URL = "https://ollama.com/search"

    private val httpClient: HttpClient by inject(named("scraper"))

    suspend fun fetchModels(
        query: String,
        page: Int,
        sort: RegistrySort = RegistrySort.POPULAR,
        category: String? = null
    ): Result<List<RegistryModel>> = runCatching {
        val html: String = withContext(Dispatchers.IO) {
            httpClient.get(buildSearchUrl(query, page, sort, category)) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
                    append("Accept", "*/*")
                    append("hx-request", "true")
                    append("hx-current-url", "https://ollama.com/search")
                }
            }.bodyAsText()
        }

        val doc = Jsoup.parse(html)
        val models = parseModelsFromHtml(doc)

        // A page with zero parsed cards is only a *legitimate* empty list when
        // the site explicitly says so (empty search results). Any other empty
        // outcome means the markup changed or the page is a challenge/error
        // page — surface an error instead of a silent "No models found".
        if (models.isEmpty() && !hasNoResultsIndicator(doc)) {
            throw RegistryParseException("no model cards parsed and page shows no empty-results indicator")
        }

        models
    }

    /**
     * Builds the ollama.com/search URL for the given query, page, sort order
     * and capability filter. Params are appended through [URLBuilder] so query
     * values are properly URL-encoded; the popular sort (the site's default)
     * and a null category simply omit their params.
     */
    internal fun buildSearchUrl(
        query: String = "",
        page: Int = 1,
        sort: RegistrySort = RegistrySort.POPULAR,
        category: String? = null
    ): String {
        val url = URLBuilder(BASE_URL)
        if (query.isNotBlank()) url.parameters.append("q", query)
        if (page > 1) url.parameters.append("page", page.toString())
        if (category != null) url.parameters.append("c", category)
        if (sort != RegistrySort.POPULAR) url.parameters.append("o", sort.toQueryParam())
        return url.buildString()
    }

    private fun RegistrySort.toQueryParam(): String = when (this) {
        RegistrySort.POPULAR -> "popular"
        RegistrySort.NEWEST -> "newest"
    }

    /**
     * True when the page explicitly reports an empty result set
     * ("No models found."). Used to distinguish a genuine empty search from a
     * markup mismatch or challenge page.
     */
    internal fun hasNoResultsIndicator(doc: Document): Boolean {
        if (doc.selectFirst("li:containsOwn(No models found)") != null) return true
        if (doc.selectFirst("p:containsOwn(No models found)") != null) return true
        return doc.text().contains("No models found", ignoreCase = true)
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
     *
     * If the primary selector matches nothing, older href-based selectors are
     * tried as a fallback to ride out minor markup churn.
     */
    internal fun parseModelsFromHtml(doc: Document): List<RegistryModel> {
        val selectors = listOf(
            "li:has(a.group.w-full)" to "a.group.w-full",
            "li:has(a[href^=\"/library/\"])" to "a[href^=\"/library/\"]",
            "li:has(a[href^=\"/i/\"])" to "a[href^=\"/i/\"]"
        )
        for ((cardSelector, linkSelector) in selectors) {
            val models = doc.select(cardSelector).mapNotNull { modelElement ->
                try {
                    parseCard(modelElement, linkSelector)
                } catch (_: Exception) {
                    null
                }
            }
            if (models.isNotEmpty()) return models
        }
        return emptyList()
    }

    private fun parseCard(modelElement: Element, linkSelector: String): RegistryModel {
        val linkElement = modelElement.selectFirst(linkSelector)
            ?: throw IllegalArgumentException("No card link found for selector: $linkSelector")
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

        return RegistryModel(
            id = id,
            name = displayName,
            description = description,
            pullCount = pullCount,
            tags = sizeTags,
            capabilities = capabilities,
            updatedAt = updatedAt
        )
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