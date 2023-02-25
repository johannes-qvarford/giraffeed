package net.qvarford.giraffeed.domain

// Get list of user ids that the user follows
// Fetch the latest videos for those user ids with period=day. Probably good to do this in parallel. Virtual threads?
// If this takes too long we will need a cache, and something that refreshes the cache on occasion.
// Keep in mind that the server restarts every day though, so maybe best to cache to normal XML file or Sqlite.
// We can even interact with the miniflux API to change the access token whenever it is re-created.
// We need to do obtain a miniflux token beforehand for that user session, unless we want others to be able to replace my access token in particular - essentially showing me their videos.

// Let's start with no caching, no parallelism and no miniflux integration.
// Token is obtained using Implicit Flow and shown for user to copy to feed username.
// Use twitch4j to call twitch.
interface TwitchFeedDownloader {
    fun downloadLatestVideosFeed(token: TwitchUserAccessToken): Feed
}

data class TwitchUserAccessToken(val value: String)
