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
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named

@ApplicationScoped
class HttpClientTwitchFeedDownloader(private val httpClient: HttpClient, private val objectMapper: ObjectMapper, @Named("multiThreadExecutorService") private val executorService: ExecutorService) : TwitchFeedDownloader {
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

        return Feed(
            updated = null,
            title = "Latest videos from the channels I follow",
            icon = URI.create("https://www.twitch.tv/favicon.ico"),
            entries = videos
                .map {
                    FeedEntry(
                        id = it.id.value,
                        author = it.userName,
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
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://api.twitch.tv/helix/videos?user_id=${userId.value}&period=day&type=archive&first=3"))
            .header("Authorization", "Bearer ${token.value}")
            .header("Client-Id", "933nb4cfbbv6rws5wo0yr2w7mjdn4g")
            .build()

        @RegisterForReflection
        data class VideosResponseData(val thumbnailUrl: String, val id: String, val userName: String, val url: String, val title: String, val publishedAt: String)
        @RegisterForReflection
        data class VideosResponse(val data: List<VideosResponseData>)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        val videosResponse = objectMapper.readValue(response.body(), VideosResponse::class.java)

        return videosResponse.data
            .map {
                Video(
                    id = VideoId(it.id),
                    userName = it.userName,
                    // TODO: It seems like the image gets broken sometimes. Wonder if this is a data race, or if there are some valid dimensions that can be used in those cases.
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

data class Video(val imageUrl: URI, val id: VideoId, val userName: String, val publicationDate: OffsetDateTime, val link: URI, val title: String)