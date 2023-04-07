package net.qvarford.giraffeed.application

import net.qvarford.giraffeed.domain.Feed
import net.qvarford.giraffeed.domain.TwitchFeedDownloader
import net.qvarford.giraffeed.domain.TwitchUserAccessToken
import jakarta.enterprise.context.ApplicationScoped

interface TwitchService {
    fun downloadLatestVideosFeed(token: TwitchUserAccessToken): Feed
}

@ApplicationScoped
class FeedDownloaderDelegatingTwitchService(private val twitchFeedDownloader: TwitchFeedDownloader) : TwitchService {
    override fun downloadLatestVideosFeed(token: TwitchUserAccessToken): Feed {
        return twitchFeedDownloader.downloadLatestVideosFeed(token)
    }
}