package net.qvarford.giraffeed.domain

import java.net.URI
import java.time.OffsetDateTime
import java.rmi.UnexpectedException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Node

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
                    link = URI.create("https://libreddit.privacy.qvarford.net").resolve(entry.link.path),
                    content = replaceRedditLinks(entry.content)
                )
            }.toList()
        )
    }

    private fun replaceRedditLinks(content: String): String {
        val document: Document = Jsoup.parse(content)

        fun recurse(node: Node) {
            if (node is TextNode) {
                node.text(replaceRedditLinksInText(node.text()))
            } else if (node is Element) {
                for (attribute in node.attributes()) {
                    node.attr(attribute.key, replaceRedditLinksInText(attribute.value))
                }
                for (child in node.childNodes()) {
                    recurse(child)
                }
            }
        }
        recurse(document)

        return document.outerHtml()
    }

    private fun replaceRedditLinksInText(text: String): String {
        return text.replaceLink("www.reddit.com", "")
            .replaceLink("i.redd.it", "/img")
            .replaceLink("preview.redd.it", "/preview/pre")
    }
}

private fun String.replaceLink(regex: String, pathPrefix: String): String {
    val r = this.replace(Regex("${regex}([^\\\"]*)"), {
        val capture = it.groupValues[1]
        "libreddit.privacy.qvarford.net${pathPrefix}${capture}"
    })
    return r
}

interface FeedDownloader {
    fun download(url: FeedUrl): Feed
}