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

// TODO: Serve underlying videos instead of gifs
//  Sometimes Libreddit serves gifs like https://libreddit.privacy.qvarford.net/preview/external-pre/abc.gif?width=640&crop=smart&s=6dab0fdd15cc8912cfaf64fd40eae080fec3a441
//  When you can find the underlying video on the libreddit page like /vid/XYZ/1080.mp4.
//  Need to investigate if it's always this way with gifs, and whether or not it just happens on /preview/external-pre/
//  Also need to verify if an mp4 is always an available source, or if we need to be able handle /hls/XYZ/HLSPlaylist.m3u8 as a fallback.
//  ALSO: The [image] gif seems to retain the quality, so maybe just replace the preview img src?
//   This may not be the case for non-imgur links
//   0.data.children.0.data.url can probably be queried

// TODO: create classes for different kinds of urls, RedditPreviewUrl, RedditImageUrl, RedditExternalPreviewUrl, LibredditPreviewUrl etc. and some good ways to turn libreddit to reddit url and vice-versa.

// TODO: use visitors / selector iterators to avoid repeating traversal code.

class LibredditFeedType(private val metadataProvider: LibredditMetadataProvider) : FeedType {
    override val name: String
        get() = "libreddit"

    override fun understandsSourceUrl(url: SourceUrl): Boolean = url.value.toString().matches(sourceUrlRegex)

    override fun extractResource(url: SourceUrl): FeedResource {
        return FeedResource(sourceUrlRegex.matchEntire(url.value.toString())!!.groupValues[1])
    }

    override fun feedUriForResource(resource: FeedResource): URI =
        URI.create("https://www.reddit.com/r/${resource.value}/hot.rss")

    override fun enhanceEntry(entry: FeedEntry): FeedEntry {
        return entry.copy(
            link = URI.create("https://libreddit.privacy.qvarford.net").resolve(entry.link.path),
            content = replaceContent(link = LibredditEntryUrl(entry.link), content = entry.content)
        )
    }

    private fun replaceContent(content: String, link: LibredditEntryUrl): String {
        val document: Document = Jsoup.parse(content)

        addMissingImageIfContainsImageLink(document)
        enhanceImagesWithMetadata(link = link, document = document)
        replaceRedditLinks(document)
        unwrapPotentialImageInTable(document)

        return document.outerHtml()
    }

    private fun addMissingImageIfContainsImageLink(document: Document) {
        var imageUrl: String? = null
        var hasImage = false

        fun recurse(node: Node) {
            if (node is Element) {
                if (node.tagName() == "img") {
                    hasImage = true
                }

                if (node.tagName() == "a" && node.attr("href").startsWith("https://i.redd.it")) {
                    imageUrl = node.attr("href")
                }

                for (attribute in node.attributes()) {
                    node.attr(attribute.key, replaceRedditLinksInText(attribute.value))
                }
                for (child in node.childNodes()) {
                    recurse(child)
                }
            }
        }
        recurse(document)

        imageUrl?.let {
            if (hasImage) return
            val img = document.createElement("img")
            img.attr("src", it)
            val br = document.createElement("br")
            val body = document.body()
            body.prependChild(br)
            body.prependChild(img)
        }
    }

    private fun replaceRedditLinks(document: Document) {
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
    }

    private fun replaceRedditLinksInText(text: String): String {
        return text.replaceLink("www.reddit.com", "")
            .replaceLink("i.redd.it", "/img")
            .replaceLink("external-preview.redd.it", "/preview/external-pre")
            .replaceLink("preview.redd.it", "/preview/pre")
            // Don't want to have to go to reddit to view the high-quality image.
            .replace(Regex("/preview/pre/(.*)\\?.*"), "/img/$1")
    }

    private fun unwrapPotentialImageInTable(document: Document) {
        fun findImg(node: Node): Element? {
            if (node is Element) {
                if (node.tagName() == "img") {
                    return node
                }
                for (child in node.childNodes()) {
                    val img = findImg(child)
                    if (img != null) {
                        return img
                    }
                }
            }
            return null
        }

        fun findTable(node: Node) {
            if (node is Element) {
                if (node.tagName() == "table") {
                    val img = findImg(node)
                    img?.let {
                        it.attr("style", "width: 740px;")
                        node.parent()!!.prependChild(it)
                    }
                    node.remove()
                    return
                }

                for (child in node.childNodes()) {
                    findTable(child)
                }
            }
        }
        findTable(document)
    }

    private fun enhanceImagesWithMetadata(document: Document, link: LibredditEntryUrl) {
        val metadata = metadataProvider.lookup(link.reddit)
        if (metadata.imageUrls.isNotEmpty()) {
            val imgElements = mutableListOf<Element>()
            fun recurse(element: Element) {
                if (element.tagName() == "img") {
                    imgElements.add(element)
                }
                for (child in element.children()) {
                    recurse(child)
                }
            }
            recurse(document)

            val alt = imgElements.firstOrNull()?.attr("alt")
            val title = imgElements.firstOrNull()?.attr("title")
            for (img in imgElements) {
                img.remove()
            }

            for (image in metadata.imageUrls) {
                val img = document.createElement("img")
                img.attr("src", image.toString())
                alt?.let {
                    if (it.isNotEmpty()) {
                        img.attr("alt", it)
                    }

                }
                title?.let {
                    if (it.isNotEmpty()) {
                        img.attr("title", it)
                    }
                }
                img.attr("style", "width: 740px;")
                document.body().prependChild(img)
            }
        }

        if (metadata.content != null) {
            // Nothing to do for now...
        }

        if (metadata.videoUrl != null) {
            document.body().children().forEach { it.remove() }

            // TODO: Standardize creation of video elements across feed types.
            val video = document.createElement("video")
            document.body().prependChild(video)
            video.attr("controls", "")
            video.attr("width", "740")
            val source = document.createElement("source")
            video.appendChild(source)
            source.attr("src", metadata.videoUrl.value.toString())
        }
    }
}

private fun String.replaceLink(regex: String, pathPrefix: String): String {
    val r = this.replace(Regex("${regex}([^\"]*)")) {
        val capture = it.groupValues[1]
        "libreddit.privacy.qvarford.net${pathPrefix}${capture}"
    }
    return r
}

data class LibredditMetadata(val imageUrls: List<URI> = emptyList(), val videoUrl: Mp4Url? = null, val content: String? = null)

data class LibredditEntryUrl(val value: URI) {
    val reddit: RedditEntryUrl
        get() = RedditEntryUrl(URI.create(value.toString().replace("libreddit.privacy.qvarford.net", "wwww.reddit.com")))
}

data class RedditEntryUrl(val value: URI)



interface LibredditMetadataProvider {
    fun lookup(entryUrl: RedditEntryUrl): LibredditMetadata
}