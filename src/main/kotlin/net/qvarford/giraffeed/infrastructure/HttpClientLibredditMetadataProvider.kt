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

    override fun lookup(entryUrl: RedditEntryUrl): LibredditMetadata {
        val root = determineT3(entryUrl);

        return if (root.isVideo) {
            LibredditMetadata(videoUrl = HlsUrl(URI.create(root.media!!.redditVideo!!.hlsUrl)).mp4Url)
        } else if (root.isGallery == true) {
            // We can't know for sure that we can use i.redd.it links.
            // We need to look up the best quality link in the metadata.

            val keys = root.galleryData!!.items.map { it.mediaId }.toList()
            val map = root.mediaMetadata!!

            val images = keys.map { URI.create(map[it]!!.s.u!!) }.toList()
            LibredditMetadata(imageUrls = images)
        } else if (root.thumbnail == "self") {
            // NOTE: root.selftext_html may be null for text posts with no content (only a title).
            // We therefor need to check selftext instead, and have a fallback for null selftext_html
            // TODO: Check if some links need to be replaced.
            LibredditMetadata(content = root.selftextHtml ?: "<p>[empty]</p>")
        }
        else {
            LibredditMetadata(imageUrls = listOf(URI.create(root.url!!)), videoUrl = null)
        }
    }

    // TODO: Better naming: what does T3 even mean?
    fun determineT3(entryUrl: RedditEntryUrl): T3 {

        // TODO: Better separation of concerns: don't mix caching with fetching
        if (cache.has(entryT3Key(entryUrl))) {
            return objectMapper.readValue(cache.get(entryT3Key(entryUrl))!!)
        }

        val subredditUrl = entryUrl.subredditUrl
        val t3s: List<T3> = if (cache.has(subredditT3sKey(subredditUrl))) {
            objectMapper.readValue(cache.get(subredditT3sKey(subredditUrl))!!)
        } else {
            val t3s = lookupSubredditT3s(subredditUrl)
            cache.put(subredditT3sKey(subredditUrl), objectMapper.writeValueAsString(t3s))
            t3s
        }

        val entryUrlString = entryUrl.value.toString()
        val t3 = t3s.firstOrNull { entryUrlString.endsWith(it.permalink) } ?: lookupEntryT3(entryUrl)

        cache.put(entryT3Key(entryUrl), objectMapper.writeValueAsString(t3))
        return t3
    }

    fun lookupSubredditT3s(subredditUrl: SubredditUrl): List<T3> {
        val request = HttpRequest.newBuilder(URI.create(String.format("%s.json", subredditUrl.value))).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val wrapper: TypeWrapper = objectMapper.readValue(response.body())
        return wrapper.data.children.map { it.data }
    }

    fun lookupEntryT3(entryUrl: RedditEntryUrl): T3 {
        // Only used during a race condition - when the hot.rss feed contains an entry that is ejected before hot.json is called.
        // By this point, the entry should already have been cached by Miniflux, but just to be sure, we use this fallback.
        val request = HttpRequest.newBuilder(URI.create(String.format("%s.json", entryUrl.value))).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val wrapper: Array<TypeWrapper> = objectMapper.readValue(response.body())
        return wrapper[0].data.children[0].data
    }

    fun entryT3Key(entryUrl: RedditEntryUrl): String = "EntryT3:${entryUrl.value}"

    fun subredditT3sKey(subredditUrl: SubredditUrl): String = "SubredditT3s:${subredditUrl.value}"
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