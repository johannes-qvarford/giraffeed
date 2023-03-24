package net.qvarford.giraffeed.domain

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Node

private val sourceUrlRegex: Regex = Regex("^(?:https://)?(?:libreddit\\.privacy.qvarford\\.net|(?:www\\.)?reddit\\.com)/r/([^/]+).*$")

// TODO: Pick up metadata for galleries: https://www.reddit.com/r/linuxhardware/comments/idarg1/mutantc_modular_and_open_source_handheld_pc_more.json
//  data.children[0].data.url includes /gallery if it is a gallery
//  detect gallery by <img src="https://b.thumbs.redditmedia.com/...> in the feed
//  data.children[0].data.media_metadata.[image_id] for the images
//  external-preview links are dereferenced, e.g. to imgur in the url property
//  run gallery fetches in parallel, replace single thumbnail with .

// TODO: Before replacing all links, change bare <a href="i.reddit.com/...>...</a> to <img src="i.reddit.com/...>...</img>, since it's a link for some bad reason.
//  Does this happen to all image links that were originally i.reddit.com?

object LibredditFeedType : FeedType {
    override val name: String
        get() = "libreddit"

    override fun understandsSourceUrl(url: SourceUrl): Boolean = url.value.toString().matches(sourceUrlRegex)

    override fun extractResource(url: SourceUrl): FeedResource {
        return FeedResource(sourceUrlRegex.matchEntire(url.value.toString())!!.groupValues[1])
    }

    override fun feedUriForResource(resource: FeedResource): URI = URI.create("https://www.reddit.com/r/${resource.value}/hot.rss")

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