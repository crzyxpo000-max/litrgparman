# Novel EPUB (scraper/)

A small Android app that downloads a web novel's chapters and packs them into an EPUB 3 file for
offline reading. It is a separate app from the game in this repository (its own `applicationId`,
`com.novelepub`) and shares only the Gradle setup.

For stories you have access to. Respect each site's terms and the authors' rights; the app fetches one
page at a time with a delay between requests.

## Use

1. Paste a novel URL (or share a page from the browser to the app).
2. **Fetch novel** reads the title, author, cover and chapter list.
3. Pick a chapter range, then **Download EPUB**.
4. **Save to…** (any folder or cloud provider through the system picker), **Open** in a reader app, or **Share**.

Downloaded chapters are cached per novel. A cancelled or failed download resumes where it stopped, and
later downloads of the same novel (for example, after new chapters come out) only fetch new chapters.
Settings: delay between requests, User-Agent, Royal Road author notes, clear cache.

## Sites

| Site | How |
| --- | --- |
| Royal Road (`royalroad.com`) | Dedicated parser. Fiction or chapter URL. Removes Royal Road's hidden anti-theft lines; author notes are optional. |
| Anything else | Generic reader: CSS selectors, each optional. |

Generic reader fields (blank = guess):

- **Chapter link selector**: run on the URL's page to build the chapter list, e.g. `ul.chapter-list a`.
  Without it, the URL must be chapter 1, and the app follows "Next" links until there are none, a link
  repeats, or **Max chapters** is reached.
- **Chapter text**: e.g. `#chapter-content`. The guess is the element with the most paragraph text.
- **Chapter title**, **Next-chapter link** (guess: `rel=next` or a link that says "Next" / "Sonraki").
- **Remove**: junk inside the text, e.g. `.ads, .chapter-nav`.

Selectors are remembered per site.

Not supported: pages that need a login, pages that build their text with JavaScript, and Cloudflare
challenge pages (they fail with an HTTP 403/503 message).

## Layout

| Module | Contents |
| --- | --- |
| `scraper/core` | Pure Kotlin/JVM: `Fetcher` (OkHttp, retries, Retry-After), `NovelSource`, `RoyalRoadSource`, `GenericSource`, `ContentCleaner` (jsoup → XHTML), `EpubWriter` (EPUB 3 + NCX, streamed), `NovelDownloader` (rate limit, cache, crawl). |
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
