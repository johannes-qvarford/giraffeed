package net.qvarford.giraffeed.domain

import java.net.URI
import java.time.OffsetDateTime

data class Feed(
    val updated: OffsetDateTime?,
    val icon: URI?,
    val title: String,
    val entries: List<FeedEntry>
)

data class FeedEntry(
    val id: String,
    val author: String,
    val link: URI,
    val published: OffsetDateTime,
    val title: String,
    val content: String,
    val contentType: String
)