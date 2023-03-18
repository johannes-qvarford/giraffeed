package net.qvarford.giraffeed.domain

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Node

private val sourceUrlRegex: Regex = Regex("^(?:https://)?(?:nitter\\.privacy.qvarford\\.net|(?:www\\.)?twitter\\.com)/([^/]+).*$")

object NitterFeedType : FeedType {
    override val name: String
        get() = "nitter"

    override fun understandsSourceUrl(url: SourceUrl): Boolean = url.value.toString().matches(sourceUrlRegex)

    override fun extractResource(url: SourceUrl): FeedResource {
        return FeedResource(sourceUrlRegex.matchEntire(url.value.toString())!!.groupValues[1])
    }

    override fun feedUriForResource(resource: FeedResource): URI = URI.create("https://nitter.privacy.qvarford.net/${resource.value}/rss")

    override fun enhance(feed: Feed): Feed {
        // expand the content html/xml and rewrite it.
        return feed.copy(
            entries = feed.entries
                .filter { entry -> entry.content.contains("<video") || entry.content.contains("<img") }
                .map { entry ->
                    entry.copy(
                        content = replaceNonMedia(entry.content)
                )}
                .toList()
        )
    }

    private fun replaceNonMedia(content: String): String {
        val document: Document = Jsoup.parse(content)

        fun recurse(node: Node) {
            if (node is TextNode) {
                node.text("")
            } else if (node is Element) {
                for (child in node.childNodes()) {
                    recurse(child)
                }
            }
        }
        recurse(document)

        return document.outerHtml()
    }
}