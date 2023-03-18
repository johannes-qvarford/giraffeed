package net.qvarford.giraffeed.domain

/**
 * Only download images, since we might as well use the normal nitter feed otherwise.
 * Consider if we should strip the text from the mixed content items or include both. I see no reason to add the context currently.
 * Maybe split multi-image items into multiple images? Probably not - sometimes the images only make sense as a set. Would be fun to fuse multiple images into one.
 */

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

    // TODO: Replace preview.redd.it with equivalent i.reddit.com links with no query params.
    //  Don't want to have to click on the post just to download the high-quality image.
    //  DOES NOT WORK WITH EXTERNAL-PREVIEW
    private fun replaceRedditLinksInText(text: String): String {
        return text.replaceLink("www.reddit.com", "")
            .replaceLink("i.redd.it", "/img")
            .replaceLink("external-preview.redd.it", "/preview/external-pre")
            .replaceLink("preview.redd.it", "/preview/pre")
            // Don't want to have to go to reddit to view the high-quality image.
            .replace(Regex("/preview/pre/(.*)\\?.*"), "/img/$1")
    }
}

private fun String.replaceLink(regex: String, pathPrefix: String): String {
    val r = this.replace(Regex("${regex}([^\"]*)")) {
        val capture = it.groupValues[1]
        "libreddit.privacy.qvarford.net${pathPrefix}${capture}"
    }
    return r
}