package net.qvarford.giraffeed.domain

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.*

private val sourceUrlRegex: Regex = Regex("^(?:https://)?(?:libreddit\\.privacy.qvarford\\.net|(?:www\\.)?reddit\\.com)/r/([^/]+).*$")

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

        enhanceImagesWithMetadata(link = link, document = document)
        replaceRedditLinks(document)

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

            for (node in document.body().childNodes()) {
                node.remove()
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
            for (node in document.body().childNodes()) {
                node.remove()
            }
            document.body().append(Entities.unescape(metadata.content))

            fun removeComments(node: Node) {
                if (node is Comment) {
                    node.remove()
                }

                if (node is Element) {
                    for (child in node.childNodes()) {
                        removeComments(child)
                    }
                }
            }
            removeComments(document)
        }

        if (metadata.videoUrl != null) {
            for (node in document.body().childNodes()) {
                node.remove()
            }

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

data class SubredditUrl(val value: URI)

private val subredditExtratorRegex = Regex("(.*/r/[^./]+).*")

data class RedditEntryUrl(val value: URI) {
    val subredditUrl: SubredditUrl
        get() = SubredditUrl(URI.create(subredditExtratorRegex.find(value.toString())!!.groupValues[1]))
}

interface LibredditMetadataProvider {
    fun lookup(entryUrl: RedditEntryUrl): LibredditMetadata
}