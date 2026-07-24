package by.ciszkin.herdmanager.data.scraping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jsoup.Jsoup

class OllamaLibraryScraperTest {

    /**
     * Mirrors the markup ollama.com serves on /search after it removed its
     * `x-test-*` test hooks: a <li> per model wrapping an /library/ link,
     * an <h2> name, a p.max-w-lg description, blue badges in div.flex-wrap
     * for size tags, and a p.my-1 stats row for pulls/updated.
     */
    private val sampleHtml = """
        <html><body>
        <ul>
          <li class="flex items-baseline border-b border-neutral-200 py-6">
            <a href="/library/ornith" class="group w-full">
              <div class="flex flex-col mb-1" title="ornith">
                <h2 class="truncate text-xl font-medium"><span>ornith</span></h2>
                <p class="max-w-lg break-words text-neutral-800 text-md">A self-improving family of open-source models for agentic coding</p>
              </div>
              <div class="flex flex-col">
                <div class="flex flex-wrap space-x-2">
                  <span class="inline-flex bg-[#ddf4ff]">9b</span>
                  <span class="inline-flex bg-[#ddf4ff]">35b</span>
                </div>
                <p class="my-1 flex space-x-5 text-[13px] font-medium text-neutral-500">
                  <span class="flex items-center"><span>287.7K</span><span class="hidden sm:flex">&nbsp;Pulls</span></span>
                  <span class="flex items-center"><span>9</span><span class="hidden sm:flex">&nbsp;Tags</span></span>
                  <span class="flex items-center" title="Jun 27, 2026 8:45 PM UTC"><span class="hidden sm:flex">Updated&nbsp;</span><span>3 weeks ago</span></span>
                </p>
              </div>
            </a>
          </li>
          <li class="flex items-baseline border-b border-neutral-200 py-6">
            <a href="/library/laguna-xs-2.1" class="group w-full">
              <div class="flex flex-col mb-1" title="laguna-xs-2.1">
                <h2 class="truncate text-xl font-medium"><span>laguna-xs-2.1</span></h2>
                <p class="max-w-lg break-words text-neutral-800 text-md">Laguna XS 2.1 is a 33B total parameter Mixture-of-Experts model.</p>
              </div>
              <div class="flex flex-col">
                <div class="flex flex-wrap space-x-2"></div>
                <p class="my-1 flex space-x-5 text-[13px] font-medium text-neutral-500">
                  <span class="flex items-center"><span>1.2M</span><span class="hidden sm:flex">&nbsp;Pulls</span></span>
                  <span class="flex items-center"><span>5</span><span class="hidden sm:flex">&nbsp;Tags</span></span>
                  <span class="flex items-center" title="Jun 20, 2026 1:00 PM UTC"><span class="hidden sm:flex">Updated&nbsp;</span><span>1 month ago</span></span>
                </p>
              </div>
            </a>
          </li>
        </ul>
        </body></html>
    """.trimIndent()

    @Test
    fun `parses every model card from the current ollama markup`() {
        val models = OllamaLibraryScraper.parseModelsFromHtml(Jsoup.parse(sampleHtml))

        assertEquals(2, models.size)

        val first = models[0]
        assertEquals("ornith", first.id)
        assertEquals("ornith", first.name)
        assertEquals("A self-improving family of open-source models for agentic coding", first.description)
        assertEquals(287_700L, first.pullCount)
        assertEquals(listOf("9b", "35b"), first.tags)
        assertTrue(first.capabilities.isEmpty(), "Capabilities are no longer exposed on the listing page")
        assertEquals("Jun 27, 2026 8:45 PM UTC", first.updatedAt)

        val second = models[1]
        assertEquals("laguna-xs-2.1", second.id)
        assertEquals("Laguna XS 2.1 is a 33B total parameter Mixture-of-Experts model.", second.description)
        assertEquals(1_200_000L, second.pullCount)
        assertTrue(second.tags.isEmpty(), "Models without size badges yield an empty tag list")
        assertEquals("Jun 20, 2026 1:00 PM UTC", second.updatedAt)
    }

    @Test
    fun `returns an empty list when the page contains no model cards`() {
        val emptyPage = Jsoup.parse("<html><body><li>some unrelated item</li></body></html>")

        val models = OllamaLibraryScraper.parseModelsFromHtml(emptyPage)

        assertTrue(models.isEmpty())
    }

    @Test
    fun `parsePullCount handles K, M and plain numbers`() {
        assertEquals(287_700L, OllamaLibraryScraper.parsePullCount("287.7K Pulls"))
        assertEquals(1_200_000L, OllamaLibraryScraper.parsePullCount("1.2M Pulls"))
        assertEquals(12_345L, OllamaLibraryScraper.parsePullCount("12,345 Pulls"))
        assertEquals(5L, OllamaLibraryScraper.parsePullCount("5"))
        assertEquals(0L, OllamaLibraryScraper.parsePullCount(""))
    }
}
