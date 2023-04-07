package net.qvarford.giraffeed.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import net.qvarford.giraffeed.domain.*
import jakarta.enterprise.context.ApplicationScoped

fun cachingFeedType(jdbiContext: JdbiContext, objectMapper: ObjectMapper, feedType: FeedType): FeedType {
    return object : FeedType {
        val cacheEnhancer = MetadataCachingFeedEnhancer(cache = jdbiContext.createCache(feedType.name), objectMapper = objectMapper, delegate = feedType)

        override val name get() = feedType.name

        override fun understandsSourceUrl(url: SourceUrl) = feedType.understandsSourceUrl(url)

        override fun extractResource(url: SourceUrl) = feedType.extractResource(url)

        override fun feedUriForResource(resource: FeedResource) = feedType.feedUriForResource(resource)

        override fun enhanceEntries(entries: List<FeedEntry>): List<FeedEntry> {
            return cacheEnhancer.enhanceEntries(entries)
        }
    }
}

@ApplicationScoped
class DelegatingFeedTypeFactory(jdbiContext: JdbiContext, objectMapper: ObjectMapper, nitterVideoUrlFactory: NitterVideoUrlFactory, libredditMetadataProvider: LibredditMetadataProvider) : FeedTypeFactory {
    val feedTypes = arrayListOf(LibredditFeedType(libredditMetadataProvider), NitterFeedType(nitterVideoUrlFactory))
        .map { cachingFeedType(jdbiContext, objectMapper, it) }
        .toList()

    override fun ofName(name: String): FeedType {
        for (type in feedTypes) {
            if (type.name == name) {
                return type
            }
        }
        throw IllegalArgumentException("name")
    }

    override fun ofSourceUrl(url: SourceUrl): FeedType {
        for (type in feedTypes) {
            if (type.understandsSourceUrl(url)) {
                return type
            }
        }
        throw IllegalArgumentException("url")
    }

}