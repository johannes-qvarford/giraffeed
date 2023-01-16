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

data class FeedResource(val value: String);

data class FeedUrl(val value: URI)

sealed interface FeedType {
    fun urlForResource(resource: FeedResource): FeedUrl
    fun enhance(feed: Feed): Feed

    companion object {
        fun ofName(name: String): FeedType {
            return when (name) {
                "libreddit" -> LibredditFeedType
                else -> throw IllegalArgumentException(name)
            }
        }
    }
}

object LibredditFeedType : FeedType {
    override fun urlForResource(resource: FeedResource): FeedUrl =
        FeedUrl(URI.create("https://reddit.com/r/${resource.value}/hot.rss"))

    override fun enhance(feed: Feed): Feed {
        // expand the content html/xml and rewrite it. Let's keep it simple with regex to replace www.reddit.com and image hosts.
        return feed.copy(
            entries = feed.entries.map { entry ->
                entry.copy(
                    link = URI.create("https://libreddit.privacy.qvarford.net").resolve(entry.link.path)
                )
            }.toList()
        )
    }
}

interface FeedDownloader {
    fun download(url: FeedUrl): Feed
}