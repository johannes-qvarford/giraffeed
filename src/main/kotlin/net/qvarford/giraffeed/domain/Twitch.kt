package net.qvarford.giraffeed.domain

// Let's start with no caching, no parallelism and no miniflux integration.
// Token is obtained using Implicit Flow and shown for user to copy to feed username.
interface TwitchFeedDownloader {
    fun downloadLatestVideosFeed(token: TwitchUserAccessToken): Feed
}

data class TwitchUserAccessToken(val value: String)
