package net.qvarford.giraffeed.domain

import java.net.URI

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

data class SourceUrl(val value: URI) {
    val feedUrl: FeedUrl
        get() {
            val type = FeedType.ofSourceUrl(this)
            val resource = type.extractResource(this)
            return FeedUrl(type, resource)
        }
}

data class FeedUrl(val type: FeedType, val resource: FeedResource) {

    val value: URI
        get() {
            return type.feedUriForResource(resource)
        }

    val proxied: ProxyUrl =
        type.proxyUrlForResource(resource)
};

data class FeedResource(val value: String)

data class ProxyUrl(val value: URI)
