package net.qvarford.giraffeed.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.runtime.annotations.RegisterForReflection
import net.qvarford.giraffeed.domain.Feed
import net.qvarford.giraffeed.domain.FeedEntry
import net.qvarford.giraffeed.domain.TwitchFeedDownloader
import net.qvarford.giraffeed.domain.TwitchUserAccessToken
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.OffsetDateTime
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class HttpClientTwitchFeedDownloader(private val httpClient: HttpClient, private val objectMapper: ObjectMapper, private val executorService: ExecutorService) : TwitchFeedDownloader {
    override fun downloadLatestVideosFeed(token: TwitchUserAccessToken): Feed {
        val users = followedBy(token, UserId("29943195"))

        val videos = executorService.submit(Callable {
            users
                .parallelStream()
                .flatMap {
                    todaysVideos(token, it).stream()
                }
                .toList()
        }).get()

        /*
            val published: OffsetDateTime,
    val title: String,
    val content: String,
    val contentType: String
         */

        return Feed(
            updated = OffsetDateTime.now(),
            title = "Latest videos from the channels I follow",
            icon = URI.create("https://www.twitch.tv/favicon.ico"),
            entries = videos
                .map {
                    FeedEntry(
                        id = it.id.value,
                        link = it.link,
                        published = it.publicationDate,
                        title =  it.title,
                        content = """
                            <a href="${it.link}">
                            <img src="${it.imageUrl}" />
                            </a>
                        """.trimIndent(),
                        contentType = "text/html"
                    )
                }
        )
    }

    private fun todaysVideos(token: TwitchUserAccessToken, userId: UserId): List<Video> {
        /*
        <rss version="2.0">
    <channel>
        <title>360Chrism's Twitch video RSS</title>
        <link>https://twitchrss.appspot.com/</link>
        <description>The RSS Feed of 360Chrism's videos on Twitch</description>
        <ttl>10</ttl>
        <item>
            <title>New Paradox Pokemon Raids!</title>
            <link>https://www.twitch.tv/videos/1751715781</link>
            <description>&lt;a href="https://www.twitch.tv/videos/1751715781"&gt;&lt;img
                src="https://vod-secure.twitch.tv/_404/404_processing_512x288.png" /&gt;&lt;/a&gt;</description>
            <guid isPermaLink="false">1751715781</guid>
            <pubDate>Tue, 28 Feb 2023 17:07:05 UT</pubDate>
            <category>archive</category>
        </item>
         */
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://api.twitch.tv/helix/videos?user_id=${userId.value}&period=day&type=archive"))
            .header("Authorization", "Bearer ${token.value}")
            .header("Client-Id", "933nb4cfbbv6rws5wo0yr2w7mjdn4g")
            .build()

        @RegisterForReflection
        data class VideosResponseData(val thumbnailUrl: String, val id: String, val url: String, val title: String, val publishedAt: String)
        @RegisterForReflection
        data class VideosResponse(val data: List<VideosResponseData>)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val videosResponse = objectMapper.readValue(response.body(), VideosResponse::class.java)

        return videosResponse.data
            .map {
                Video(
                    id = VideoId(it.id),
                    imageUrl = URI.create(it.thumbnailUrl.replace("%{width}", "512").replace("%{height}", "288")),
                    link = URI.create(it.url),
                    publicationDate = OffsetDateTime.parse(it.publishedAt),
                    title = it.title)
            }
            .toList()
    }

    private fun followedBy(token: TwitchUserAccessToken, userId: UserId): List<UserId> {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://api.twitch.tv/helix/channels/followed?user_id=${userId.value}"))
            .header("Authorization", "Bearer ${token.value}")
            .header("Client-Id", "933nb4cfbbv6rws5wo0yr2w7mjdn4g")
            .build()

        @RegisterForReflection
        data class FollowedResponseData(val broadcasterId: String)
        @RegisterForReflection
        data class FollowedResponse(val data: List<FollowedResponseData>)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val followedResponse = objectMapper.readValue(response.body(), FollowedResponse::class.java)

        return followedResponse.data
            .map { UserId(it.broadcasterId) }
            .toList()
    }
}

data class UserId(val value: String)

data class VideoId(val value: String)

data class Video(val imageUrl: URI, val id: VideoId, val publicationDate: OffsetDateTime, val link: URI, val title: String)