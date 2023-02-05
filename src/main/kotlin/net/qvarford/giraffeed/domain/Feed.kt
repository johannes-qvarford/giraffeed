package net.qvarford.giraffeed.domain

import java.net.URI
import java.time.OffsetDateTime

data class Feed(
    val updated: OffsetDateTime,
    val icon: URI,
    val title: String,
    val entries: List<FeedEntry>
)

data class FeedEntry(
    // TODO: Add author
    val id: String,
    val link: URI,
    val published: OffsetDateTime,
    val title: String,
    val content: String,
    val contentType: String
)

data class FeedResource(val value: String)

data class SourceUrl(val value: URI) {
    val feedUrl: FeedUrl
        get() {
            val type = FeedType.ofSourceUrl(this)
            val resource = type.extractResource(this)
            return FeedUrl(type, resource)
        }
}

data class ProxyUrl(val value: URI)

data class FeedUrl(val type: FeedType, val resource: FeedResource) {

    val value: URI
        get() {
            return type.feedUriForResource(resource)
        }

    val proxied: ProxyUrl =
        type.proxyUrlForResource(resource)
};

sealed interface FeedType {
    val name: String
    fun understandsSourceUrl(url: SourceUrl): Boolean;
    fun enhance(feed: Feed): Feed
    fun extractResource(url: SourceUrl): FeedResource
    fun feedUriForResource(resource: FeedResource): URI

    fun proxyUrlForResource(resource: FeedResource): ProxyUrl {
        return ProxyUrl(URI.create("https://giraffeed.privacy.qvarford.net/enhancement/${name}/${resource.value}"))
    }

    companion object {
        fun ofName(name: String): FeedType {
            return when (name) {
                LibredditFeedType.name -> LibredditFeedType
                else -> throw IllegalArgumentException("name")
            }
        }

        fun ofSourceUrl(url: SourceUrl): FeedType {
            return LibredditFeedType
        }
    }
}

interface FeedDownloader {
    fun download(url: FeedUrl): Feed
}