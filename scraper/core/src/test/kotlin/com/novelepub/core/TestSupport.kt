package com.novelepub.core

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.getResource("/fixtures/$name")) { "missing fixture $name" }.readText()

fun fixtureDoc(name: String, url: String): Document = Jsoup.parse(fixture(name), url)

/** Serves canned pages; records every requested URL. */
class FakeFetcher(private val pages: Map<String, String>) : Fetcher {
    val requests = ArrayList<String>()
    override suspend fun get(url: String): FetchResult {
        requests += url
        val body = pages[url] ?: throw HttpStatusException(url, 404)
        return FetchResult(url, body.toByteArray(), "text/html; charset=utf-8")
    }
}

class FailingFetcher : Fetcher {
    override suspend fun get(url: String): FetchResult = throw IOException("offline")
}

/** Throws if [xml] is not well-formed XML. */
fun assertWellFormed(xml: String, name: String = "document") {
    val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    try {
        factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    } catch (e: Exception) {
        throw AssertionError("$name is not well-formed XML: ${e.message}\n$xml", e)
    }
}
