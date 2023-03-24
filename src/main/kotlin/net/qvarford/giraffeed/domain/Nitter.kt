package net.qvarford.giraffeed.domain

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Node

private val sourceUrlRegex: Regex = Regex("^(?:https://)?(?:nitter\\.privacy.qvarford\\.net|(?:www\\.)?twitter\\.com)/([^/]+).*$")

// TODO: Include videos.
//  This is really complicated. We can identify videos with <img src="...ext_tw_video_thumb...">
//  We can either add the HLS player to miniflux, and just include a bit of code to be able to play the file.
//  Or, download the HLS stream on the server, convert it to mp4, save to temporary storage / file system, and then serve the generated file.
//  example: ffmpeg -i https://nitter.privacy.qvarford.net/video/B145328B60AF6/https%3A%2F%2Fvideo.twimg.com%2Famplify_video%2F1635778493685420038%2Fpl%2F3rLqpVWkgBJsSEc4.m3u8%3Ftag%3D16%26container%3Dfmp4 -c copy -bsf:a aac_adtstoasc -movflags frag_keyframe+empty_moov output3.mp4
//  WE CAN'T store the generated file temporarily when the feed gets pulled - some feeds aren't checked for days - it has to be durable, or generated when GET:ing the mp4 link, but generating it will take too much time probably.
//  https://stackoverflow.com/questions/32528595/ffmpeg-mp4-from-http-live-streaming-m3u8-file
//  First try to include a video element that points to an online mp4, and see if it gets played in miniflux
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