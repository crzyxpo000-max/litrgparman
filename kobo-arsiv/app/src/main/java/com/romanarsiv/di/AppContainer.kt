package com.romanarsiv.di

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import com.romanarsiv.data.remote.BingSearchProvider
import com.romanarsiv.data.remote.ChallengeBroker
import com.romanarsiv.data.remote.DuckDuckGoSearchProvider
import com.romanarsiv.data.remote.FallbackPageLoader
import com.romanarsiv.data.remote.JsoupPageLoader
import com.romanarsiv.data.remote.WebViewPageLoader
import com.romanarsiv.data.repository.NovelRepositoryImpl
import com.romanarsiv.data.storage.FileHelper
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.SearchNovelUseCase
import com.romanarsiv.epub.EpubGenerator

/** Elle bağımlılık enjeksiyonu; tek uygulama, tek ekran için Hilt'e gerek yok. */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    /** WebView doğrulama sayfasını arayüze taşır. */
    val challengeBroker = ChallengeBroker()

    // Jsoup, WebView ile aynı User-Agent'ı ve çerezleri kullanır: doğrulamadan geçen tarayıcının
    // çerezi (ör. cf_clearance) yalnızca aynı User-Agent ile geçerlidir.
    private val userAgent: String by lazy {
        runCatching { WebSettings.getDefaultUserAgent(app) }.getOrNull() ?: JsoupPageLoader.USER_AGENT
    }

    private val loader = FallbackPageLoader(
        http = JsoupPageLoader(
            userAgent = { userAgent },
            cookies = { url -> runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull() },
        ),
        browser = WebViewPageLoader(app),
        solveChallenge = challengeBroker::requestSolve,
    )

    private val repository: NovelRepository = NovelRepositoryImpl(
        loader = loader,
        providers = listOf(DuckDuckGoSearchProvider(), BingSearchProvider()),
    )
    private val storage = FileHelper(app)
    private val generator = EpubGenerator(kepub = true)

    val searchNovel = SearchNovelUseCase(repository)
    val archiveNovel = ArchiveNovelUseCase(repository, generator, storage)
}
