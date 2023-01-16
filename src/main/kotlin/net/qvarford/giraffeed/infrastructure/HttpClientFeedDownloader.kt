package net.qvarford.giraffeed.infrastructure

import net.qvarford.giraffeed.domain.Feed
import net.qvarford.giraffeed.domain.FeedDownloader
import net.qvarford.giraffeed.domain.FeedUrl
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.InternalServerErrorException


@ApplicationScoped
class HttpClientFeedDownloader(private val client: HttpClient, private val converter: FeedConverter): FeedDownloader {

    override fun download(url: FeedUrl): Feed {
        val response = client.send(HttpRequest.newBuilder(url.value).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            throw InternalServerErrorException("Invalid status code: " + response.statusCode())
        }
        return response.body().use { converter.xmlStreamToFeed(it) }
    }
}