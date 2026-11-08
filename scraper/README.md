# Novel EPUB (scraper/)

A small Android app that downloads a web novel's chapters and packs them into an EPUB 3 file for
offline reading. It is a separate app from the game in this repository (its own `applicationId`,
`com.novelepub`) and shares only the Gradle setup.

For stories you have access to. Respect each site's terms and the authors' rights; the app fetches one
page at a time with a delay between requests.

## Use

1. **Search** tab: type the novel's title. The app lists the pages it found, one card per site
   (sites with a dedicated parser first). Tap one. Or, in the **URL** tab, paste an address
   (or share a page from the browser to the app) and tap **Fetch novel**.
2. The app reads the title, author, cover and chapter list.
3. Pick a chapter range, then **Download EPUB**.
4. **Save to…** (any folder or cloud provider through the system picker), **Open** in a reader app, or **Share**.

Downloaded chapters are cached per novel. A cancelled or failed download resumes where it stopped, and
later downloads of the same novel (for example, after new chapters come out) only fetch new chapters.
Settings: delay between requests, User-Agent, Royal Road author notes, clear cache.

## Search

| Provider | Needs | Notes |
| --- | --- | --- |
| Royal Road site search | nothing | Always on. |
| [Brave Search API](https://brave.com/search/api/) | your API key (Settings) | Whole web. Brave asks for a card; a small monthly credit is free. |
| [SearXNG](https://docs.searxng.org/) | instance address (Settings) | Whole web. The instance must allow `format=json` (most public ones do not; a self-hosted one does). |

Google is not used: its Custom Search JSON API is closed to new customers and shuts down on
2027-01-01, and scraping google.com is against its terms. Providers run in parallel; results are
merged, duplicates and book shops/wikis/social sites dropped (`SearchService.NOT_READABLE`), and a
failing provider is shown as an error line without hiding the others' results.

## Sites

| Site | How |
| --- | --- |
| Royal Road (`royalroad.com`) | Dedicated parser. Fiction or chapter URL. Removes Royal Road's hidden anti-theft lines; author notes are optional. |
| Anything else | Generic reader: CSS selectors, each optional. |

Generic reader fields (blank = guess):

- **Chapter link selector**: run on the URL's page to build the chapter list, e.g. `ul.chapter-list a`.
  Blank: the app looks for the biggest group of chapter-like links ("Chapter 12", "Bölüm 3",
  `.../chapter-12`). Paginated lists are followed (a "next" link or the page after the active one), and
  newest-first lists are reversed. If the page has no such list, or is itself a chapter, the app
  follows "Next" links from it until there are none, a link repeats, or **Max chapters** is reached.
  When a guessed list looks wrong, **Start from the first chapter and follow "Next" links** switches to that.
- **Chapter text**: e.g. `#chapter-content`. The guess is the element with the most paragraph text.
- **Chapter title**, **Next-chapter link** (guess: `rel=next` or a link that says "Next" / "Sonraki").
- **Remove**: junk inside the text, e.g. `.ads, .chapter-nav`.

Selectors are remembered per site.

Not supported: pages that need a login, pages that build their text with JavaScript, and Cloudflare
challenge pages (they fail with an HTTP 403/503 message).

## Layout

| Module | Contents |
| --- | --- |
| `scraper/core` | Pure Kotlin/JVM: `Fetcher` (OkHttp, retries, Retry-After), `Search` (providers, `SearchService`), `NovelSource`, `RoyalRoadSource`, `GenericSource`, `ContentCleaner` (jsoup → XHTML), `EpubWriter` (EPUB 3 + NCX, streamed), `NovelDownloader` (rate limit, cache, crawl). |
| `scraper/app` | Compose UI and `MainViewModel` only. English and Turkish strings. |

Adding a site: implement `SiteSource` in `core` (see `RoyalRoadSource`), add it to `Sources.sites`, and add
HTML fixtures + tests under `core/src/test`.

## Commands

```bash
./gradlew :scraper:core:test          # unit tests; also writes scraper/core/build/sample.epub
./gradlew :scraper:app:assembleDebug  # APK: scraper/app/build/outputs/apk/debug/app-debug.apk
./gradlew :scraper:app:installDebug   # install on a connected phone
./gradlew :scraper:app:lintDebug
```
