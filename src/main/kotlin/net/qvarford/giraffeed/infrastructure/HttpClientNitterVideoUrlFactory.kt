package net.qvarford.giraffeed.infrastructure

import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.NitterEntryUrl
import net.qvarford.giraffeed.domain.NitterVideoUrlFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpClientNitterVideoUrlFactory(private val httpClient: HttpClient): NitterVideoUrlFactory {
    override fun lookup(id: NitterEntryUrl): HlsUrl {
        val request = HttpRequest.newBuilder(id.value).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() != 200) {
            throw Exception("Could not lookup nitter entry: ${id.value}. Status code: ${response.statusCode()}")
        }

        val document: Document = Jsoup.parse(response.body(), "UTF-8", "")

        val elements = document.select(".main-thread .attachments video")
        assert(elements.count() == 1)

        val path = elements[0].attr("data-url")
        val url = id.value.resolve(path)
        return HlsUrl(url)
    }
}