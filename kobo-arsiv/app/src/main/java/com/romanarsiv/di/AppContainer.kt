package com.romanarsiv.di

import android.content.Context
import com.romanarsiv.data.remote.BingSearchProvider
import com.romanarsiv.data.remote.DuckDuckGoSearchProvider
import com.romanarsiv.data.remote.JsoupPageLoader
import com.romanarsiv.data.repository.NovelRepositoryImpl
import com.romanarsiv.data.storage.FileHelper
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.SearchNovelUseCase
import com.romanarsiv.epub.EpubGenerator

/** Elle bağımlılık enjeksiyonu; tek uygulama, tek ekran için Hilt'e gerek yok. */
class AppContainer(context: Context) {
    private val repository: NovelRepository = NovelRepositoryImpl(
        loader = JsoupPageLoader(),
        providers = listOf(DuckDuckGoSearchProvider(), BingSearchProvider()),
    )
    private val storage = FileHelper(context.applicationContext)
    private val generator = EpubGenerator(kepub = true)

    val searchNovel = SearchNovelUseCase(repository)
    val archiveNovel = ArchiveNovelUseCase(repository, generator, storage)
}
