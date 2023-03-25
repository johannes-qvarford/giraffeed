package net.qvarford.giraffeed.domain

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Node

private val sourceUrlRegex: Regex = Regex("^(?:https://)?(?:nitter\\.privacy.qvarford\\.net|(?:www\\.)?twitter\\.com)/([^/]+).*$")

class NitterFeedType(private val videoUrlFactory: NitterVideoUrlFactory) : FeedType {
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
                        content = replaceContent(id = entry.id, content = entry.content)
                )}
                .toList()
        )
    }

    private fun replaceContent(id: String, content: String): String {
        val document: Document = Jsoup.parse(content)

        replaceNonMedia(document)
        replaceVideoThumbnailWithVideo(id = id, document = document)

        return document.outerHtml()
    }

    private fun replaceNonMedia(document: Document) {
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
    }

    private fun replaceVideoThumbnailWithVideo(id: String, document: Document) {
        fun recurse(node: Node) {
            if (node is Element) {
                if (node.tagName() == "img" && node.attr("src").contains("video_thumb")) {
                    val parent = node.parent()!!

                    val video = document.createElement("video")
                    parent.appendChild(video)
                    video.attr("controls", "")
                    video.attr("width", "740")
                    val source = document.createElement("source")
                    video.appendChild(source)
                    source.attr("src", fetchVideoUrl(id))

                    node.remove()

                    return
                }
                for (child in node.childNodes()) {
                    recurse(child)
                }
            }
        }
        recurse(document)
    }

    private fun fetchVideoUrl(id: String): String {
        return videoUrlFactory.lookup(NitterEntryUrl(URI.create(id))).mp4Url.value.toString()
    }
}

data class NitterEntryUrl(val value: URI)

interface NitterVideoUrlFactory {
    fun lookup(id: NitterEntryUrl): HlsUrl
}