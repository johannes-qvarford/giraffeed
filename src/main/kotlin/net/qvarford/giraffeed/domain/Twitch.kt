package net.qvarford.giraffeed.domain

interface TwitchFeedDownloader {
    fun downloadLatestVideosFeed(token: TwitchUserAccessToken): Feed
}

data class TwitchUserAccessToken(val value: String)
