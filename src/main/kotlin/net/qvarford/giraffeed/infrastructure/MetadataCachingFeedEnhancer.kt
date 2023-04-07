package net.qvarford.giraffeed.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.qvarford.giraffeed.domain.FeedEnhancer
import net.qvarford.giraffeed.domain.FeedEntry

class MetadataCachingFeedEnhancer(private val cache: MetadataCache, private val objectMapper: ObjectMapper, private val delegate: FeedEnhancer) : FeedEnhancer {
    override fun enhanceEntries(entries: List<FeedEntry>): List<FeedEntry> {
        return entries
            .filter { delegate.shouldIncludeEntry(it) }
            .map {
                val value = cache.get(it.id)
                return@map if (value == null) {
                    val newValue = delegate.enhanceEntry(it)
                    cache.put(it.id, objectMapper.writeValueAsString(newValue))
                    newValue
                } else {
                    objectMapper.readValue(value)
                }
            }
            .toList()
    }


}
