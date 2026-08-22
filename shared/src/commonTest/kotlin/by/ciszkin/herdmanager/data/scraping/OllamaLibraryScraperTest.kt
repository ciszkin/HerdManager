package by.ciszkin.herdmanager.data.scraping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jsoup.Jsoup

class OllamaLibraryScraperTest {

    /**
     * Mirrors the markup ollama.com serves on /search after it removed its
     * `x-test-*` test hooks: a <li> per model wrapping a card link (the
     * `group w-full` anchor), an <h2> name, a p.max-w-lg description,
     * color-coded badges in div.flex-wrap (text-blue-600 = size,
     * text-indigo-600 = capability, text-cyan-500 = cloud availability
     * flag), and a p.my-1 stats row for pulls/updated.
     *
     * Library models link to `/library/<name>`; community models link to
     * `/<user>/<model>`. The same card markup wraps both, so the parser
     * must pick up community cards too (a search for "bonsai" otherwise
     * drops every real result).
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
                  <span class="inline-flex items-center rounded-md bg-indigo-50 px-2 text-xs font-medium text-indigo-600">vision</span>
                  <span class="inline-flex items-center rounded-md bg-indigo-50 px-2 text-xs font-medium text-indigo-600">tools</span>
                  <span class="inline-flex items-center rounded-md bg-indigo-50 px-2 text-xs font-medium text-indigo-600">thinking</span>
                  <span class="inline-flex items-center rounded-md bg-cyan-50 px-2 text-xs font-medium text-cyan-500">cloud</span>
                  <span class="inline-flex items-center rounded-md bg-[#ddf4ff] px-2 text-xs font-medium text-blue-600">9b</span>
                  <span class="inline-flex items-center rounded-md bg-[#ddf4ff] px-2 text-xs font-medium text-blue-600">35b</span>
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
          <li class="flex items-baseline border-b border-neutral-200 py-6">
            <a href="/MobiusDevelopment/Bonsai-27B-Q1_0-gguf" class="group w-full">
              <div class="flex flex-col mb-1" title="Bonsai-27B-Q1_0-gguf">
                <h2 class="truncate text-xl font-medium"><span>MobiusDevelopment/Bonsai-27B-Q1_0-gguf</span></h2>
                <p class="max-w-lg break-words text-neutral-800 text-md">Full 27B-class reasoning in binary transformer weights.</p>
              </div>
              <div class="flex flex-col">
                <div class="flex flex-wrap space-x-2">
                  <span class="inline-flex my-1 items-center rounded-md bg-indigo-50 px-2 py-[2px] text-xs font-medium text-indigo-600">vision</span>
                </div>
                <p class="my-1 flex space-x-5 text-[13px] font-medium text-neutral-500">
                  <span class="flex items-center"><span>745</span><span class="hidden sm:flex">&nbsp;Pulls</span></span>
                  <span class="flex items-center"><span>1</span><span class="hidden sm:flex">&nbsp;Tag</span></span>
                  <span class="flex items-center" title="Jul 20, 2026 8:06 PM UTC"><span class="hidden sm:flex">Updated&nbsp;</span><span>6 days ago</span></span>
                </p>
              </div>
            </a>
          </li>
        </ul>
        </body></html>
    """.trimIndent()

    @Test
    fun `parses library and community model cards from the current ollama markup`() {
        val models = OllamaLibraryScraper.parseModelsFromHtml(Jsoup.parse(sampleHtml))

        assertEquals(3, models.size)

        val first = models[0]
        assertEquals("ornith", first.id)
        assertEquals("ornith", first.name)
        assertEquals("A self-improving family of open-source models for agentic coding", first.description)
        assertEquals(287_700L, first.pullCount)
        assertEquals(listOf("9b", "35b"), first.tags)
        assertEquals(listOf("vision", "tools", "thinking"), first.capabilities)
        assertEquals("Jun 27, 2026 8:45 PM UTC", first.updatedAt)

        val second = models[1]
        assertEquals("laguna-xs-2.1", second.id)
        assertEquals("Laguna XS 2.1 is a 33B total parameter Mixture-of-Experts model.", second.description)
        assertEquals(1_200_000L, second.pullCount)
        assertTrue(second.tags.isEmpty(), "Models without size badges yield an empty tag list")
        assertEquals("Jun 20, 2026 1:00 PM UTC", second.updatedAt)

        val community = models[2]
        assertEquals("MobiusDevelopment/Bonsai-27B-Q1_0-gguf", community.id)
        assertEquals("MobiusDevelopment/Bonsai-27B-Q1_0-gguf", community.name)
        assertEquals("Full 27B-class reasoning in binary transformer weights.", community.description)
        assertEquals(745L, community.pullCount)
        assertTrue(community.tags.isEmpty(), "Community cards with no size badges yield an empty tag list")
        assertEquals(listOf("vision"), community.capabilities)
        assertEquals("Jul 20, 2026 8:06 PM UTC", community.updatedAt)
    }

    @Test
    fun `returns an empty list when the page contains no model cards`() {
        val emptyPage = Jsoup.parse("<html><body><li>some unrelated item</li></body></html>")

        val models = OllamaLibraryScraper.parseModelsFromHtml(emptyPage)

        assertTrue(models.isEmpty())
    }

    @Test
    fun `detects genuine empty search results via no models found indicator`() {
        val emptyResultsPage = Jsoup.parse(
            """
            <html><body>
            <ul role="list" class="grid grid-cols-1">
              <li class="flex py-6 px-2 text-neutral-500">No models found.</li>
            </ul>
            </body></html>
            """.trimIndent()
        )

        assertTrue(OllamaLibraryScraper.hasNoResultsIndicator(emptyResultsPage))
    }

    @Test
    fun `does not treat a challenge or markup-changed page as empty results`() {
        val challengePage = Jsoup.parse(
            """
            <html><body>
            <div class="challenge">
              <h1>Just a moment...</h1>
              <p>Checking your browser before accessing.</p>
            </div>
            </body></html>
            """.trimIndent()
        )

        assertTrue(OllamaLibraryScraper.parseModelsFromHtml(challengePage).isEmpty())
        assertTrue(!OllamaLibraryScraper.hasNoResultsIndicator(challengePage))
    }

    @Test
    fun `parses library cards via fallback selector when primary card anchor is missing`() {
        // Simulates markup churn: the li cards exist and link to /library/, but
        // the `group w-full` anchor class is gone.
        val legacyMarkup = Jsoup.parse(
            """
            <html><body>
            <ul role="list" class="grid grid-cols-1">
              <li class="flex items-baseline border-b border-neutral-200 py-6">
                <a href="/library/ornith">
                  <div class="flex flex-col mb-1">
                    <h2 class="truncate text-xl font-medium"><span>ornith</span></h2>
                    <p class="max-w-lg break-words text-neutral-800 text-md">A self-improving model.</p>
                  </div>
                  <div class="flex flex-col">
                    <div class="flex flex-wrap space-x-2">
                      <span class="inline-flex my-1 items-center rounded-md bg-[#ddf4ff] px-2 py-[2px] text-xs font-medium text-blue-600">9b</span>
                    </div>
                    <p class="my-1 flex space-x-5 text-[13px] font-medium text-neutral-500">
                      <span class="flex items-center"><span>287.7K</span><span class="hidden sm:flex">&nbsp;Pulls</span></span>
                    </p>
                  </div>
                </a>
              </li>
            </ul>
            </body></html>
            """.trimIndent()
        )

        val models = OllamaLibraryScraper.parseModelsFromHtml(legacyMarkup)

        assertEquals(1, models.size)
        assertEquals("ornith", models[0].id)
        assertEquals("A self-improving model.", models[0].description)
        assertEquals(287_700L, models[0].pullCount)
    }

    @Test
    fun `parses community cards via i-slash fallback when primary card anchor is missing`() {
        val slashMarkup = Jsoup.parse(
            """
            <html><body>
            <ul role="list" class="grid grid-cols-1">
              <li class="flex items-baseline border-b border-neutral-200 py-6">
                <a href="/i/MobiusDevelopment-Bonsai-27B-Q1_0">
                  <div class="flex flex-col mb-1">
                    <h2 class="truncate text-xl font-medium"><span>MobiusDevelopment/Bonsai-27B</span></h2>
                    <p class="max-w-lg break-words text-neutral-800 text-md">Full 27B-class reasoning.</p>
                  </div>
                </a>
              </li>
            </ul>
            </body></html>
            """.trimIndent()
        )

        val models = OllamaLibraryScraper.parseModelsFromHtml(slashMarkup)

        assertEquals(1, models.size)
        assertEquals("MobiusDevelopment-Bonsai-27B-Q1_0", models[0].id)
        assertEquals("MobiusDevelopment/Bonsai-27B", models[0].name)
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
