package net.qvarford.giraffeed.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.enterprise.context.ApplicationScoped
import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.RedditEntryUrl
import net.qvarford.giraffeed.domain.LibredditMetadata
import net.qvarford.giraffeed.domain.LibredditMetadataProvider
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@ApplicationScoped
class HttpClientLibredditMetadataProvider(private val httpClient: HttpClient, private val objectMapper: ObjectMapper) : LibredditMetadataProvider {
    override fun lookup(entryUrl: RedditEntryUrl): LibredditMetadata {
        val request = HttpRequest.newBuilder(URI.create(String.format("%s.json", entryUrl.value))).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        val wrapper: Array<TypeWrapper> = objectMapper.readValue(response.body())
        val root = wrapper[0].data.children[0].data

        return if (root.isVideo) {
            LibredditMetadata(videoUrl = HlsUrl(URI.create(root.media!!.redditVideo!!.hlsUrl)).mp4Url)
        } else if (root.isGallery == true) {
            val images = root.galleryData!!.items.map { URI.create("https://i.redd.it/${it.mediaId}") }.toList()
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

data class T3(val isVideo: Boolean, val isGallery: Boolean?, val media: Media?, val galleryData: GalleryData?, val url: String?, val selftext: String?, val selftextHtml: String?, val thumbnail: String?)

data class Media(val redditVideo: RedditVideo?)

data class RedditVideo(val hlsUrl: String)

data class GalleryData(val items: List<GalleryMedia>)

data class GalleryMedia(val mediaId: String)