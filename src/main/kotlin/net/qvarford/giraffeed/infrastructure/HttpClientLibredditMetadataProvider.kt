package net.qvarford.giraffeed.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.enterprise.context.ApplicationScoped
import net.qvarford.giraffeed.domain.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@ApplicationScoped
class HttpClientLibredditMetadataProvider(private val httpClient: HttpClient, private val objectMapper: ObjectMapper, jdbiContext: JdbiContext) : LibredditMetadataProvider {
    private val cache = jdbiContext.createCache("HttpClientLibredditMetadataProvider")

    // TODO: Consider using the json feed directly, instead of first fetching the rss.
    override fun lookup(entryUrl: RedditEntryUrl): LibredditMetadata {
        val root = determineT3(entryUrl)

        return if (root.isVideo) {
            LibredditMetadata(videoUrl = HlsUrl(URI.create(root.media!!.redditVideo!!.hlsUrl)).mp4Url)
        } else if (root.isGallery == true) {
            // We can't know for sure that we can use i.redd.it links.
            // We need to look up the best quality link in the metadata.

            val keys = root.galleryData!!.items.map { it.mediaId }.toList()
            val map = root.mediaMetadata!!

            val images = keys.map { URI.create(map[it]!!.s.u!!) }.toList()
            LibredditMetadata(imageUrls = images)
        } else {
            val urlIsMedia = root.url!!.contains("i.redd.it")
                    || root.url.contains("external-preview.redd.it")
                    || root.url.contains("preview.redd.it")
            val urlIsSelf = root.url.endsWith(root.permalink) ?: false
            val isCrossPost = isCrossPost(root)

            val content =
                if (isCrossPost) {
                    "<a href=\"${root.url}\">Cross Post</a>"
                } else if (root.selftext != "" || root.thumbnail == "self" || !urlIsMedia) {
                    (root.selftextHtml ?: "<a href=\"${root.url}\">Link</a>") ?: "<p>[empty]</p>"
                } else { null }

            val imageUrls = if (urlIsSelf || !urlIsMedia) { listOf() } else { listOf(URI.create(root.url!!)) }

            LibredditMetadata(imageUrls = imageUrls, videoUrl = null, html = content)
        }
    }

    private val subredditRegex = Regex("/r/([^/]*)")

    private fun isCrossPost(root: T3): Boolean {
        val subreddit = subredditRegex.find(root.permalink)
            ?.groupValues?.get(1)
        val linkedSubreddit = subredditRegex.find(root.url!!)
            ?.groupValues?.get(1)

        return subreddit != null && linkedSubreddit != null && subreddit != linkedSubreddit
    }

    // TODO: Better naming: what does T3 even mean?
    private fun determineT3(entryUrl: RedditEntryUrl): T3 {

        val key = entryT3Key(entryUrl)
        // TODO: Better separation of concerns: don't mix caching with fetching
        if (!cache.has(key)) {

            val subredditUrl = entryUrl.subredditUrl
            val t3s = lookupSubredditT3s(subredditUrl)
            t3s.forEach {
                val t3key = entryT3Key(RedditEntryUrl(URI.create("https://www.reddit.com${it.permalink}")))
                if (!cache.has(t3key)) {
                    cache.put(t3key, objectMapper.writeValueAsString(it))
                }
            }

            if (!cache.has(key)) {
                val t3 = lookupEntryT3(entryUrl)
                cache.put(key, objectMapper.writeValueAsString(t3))
            }
        }

        return objectMapper.readValue(cache.get(entryT3Key(entryUrl))!!)
    }

    private fun lookupSubredditT3s(subredditUrl: SubredditUrl): List<T3> {
        val request = HttpRequest.newBuilder(URI.create(String.format("%s/hot.json", subredditUrl.value))).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val wrapper: TypeWrapper = objectMapper.readValue(response.body())
        return wrapper.data.children.map { it.data }
    }

    private fun lookupEntryT3(entryUrl: RedditEntryUrl): T3 {
        // Only used during a race condition - when the hot.rss feed contains an entry that is ejected before hot.json is called.
        // By this point, the entry should already have been cached by Miniflux, but just to be sure, we use this fallback.
        val request = HttpRequest.newBuilder(URI.create(String.format("%s.json", entryUrl.value))).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val wrapper: Array<TypeWrapper> = objectMapper.readValue(response.body())
        return wrapper[0].data.children[0].data
    }

    private fun entryT3Key(entryUrl: RedditEntryUrl): String = "EntryT3:${entryUrl.value}"
}

// Should we trust the metadata completely?
// Or, should we only trust the metadata when we know we are dealing with weird stuff?
// Replace the existing <img>, by adding 1 or more siblings and then removing the img.

// root: 0.data.children.0.data
// detect video with is_video (media.reddit_video.hls_url) // media not present/null for gallery or non-video
// detect gallery with is_gallery (extract gallery_data.items.[].media_id use as postfix for https://i.redd.it/X) // gallery_data only present for galleries
// otherwise (url)
// /preview -> 0.data.children.0.data.url (e.g. https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10ayn48/guys_i_fixed_it.json)

data class TypeWrapper(val data: Listing)

data class Listing(val children: List<ListingChild>)

data class ListingChild(val data: T3)

data class T3(val isVideo: Boolean, val isGallery: Boolean?, val media: Media?, val galleryData: GalleryData?, val mediaMetadata: Map<String, MediaMetaDataEntry>?, val url: String?, val selftext: String?, val selftextHtml: String?, val thumbnail: String?, val permalink: String)

data class Media(val redditVideo: RedditVideo?)

data class RedditVideo(val hlsUrl: String)

data class GalleryData(val items: List<GalleryMedia>)

data class GalleryMedia(val mediaId: String)

data class MediaMetaDataEntry(val s: MediaMetaDataSize)

data class MediaMetaDataSize(val u: String?)