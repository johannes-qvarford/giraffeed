package net.qvarford.giraffeed.domain

import java.net.URI

interface FeedEnhancer {
    fun enhance(feed: Feed): Feed {
        return feed.copy(
            entries = enhanceEntries(feed.entries)
        )
    }

    fun enhanceEntries(entries: List<FeedEntry>): List<FeedEntry> {
        return entries.filter { shouldIncludeEntry(it) }.map { enhanceEntry(it) }.toList()
    }

    fun shouldIncludeEntry(entry: FeedEntry) = true

    fun enhanceEntry(entry: FeedEntry) = entry
}

interface FeedType : FeedEnhancer {
    val name: String
    fun understandsSourceUrl(url: SourceUrl): Boolean
    fun extractResource(url: SourceUrl): FeedResource
    fun feedUriForResource(resource: FeedResource): URI

    fun proxyUrlForResource(resource: FeedResource): ProxyUrl {
        return ProxyUrl(URI.create("https://giraffeed.privacy.qvarford.net/enhancement/${name}/${resource.value}"))
    }
}

interface FeedTypeFactory {
    fun ofName(name: String): FeedType

    fun ofSourceUrl(url: SourceUrl): FeedType
}

interface FeedDownloader {
    fun download(url: FeedUrl): Feed
}

data class SourceUrl(val value: URI)

data class FeedUrl(val type: FeedType, val resource: FeedResource) {

    val value: URI
        get() {
            return type.feedUriForResource(resource)
        }

    val proxied: ProxyUrl =
        type.proxyUrlForResource(resource)
}

data class FeedResource(val value: String)

data class ProxyUrl(val value: URI)
