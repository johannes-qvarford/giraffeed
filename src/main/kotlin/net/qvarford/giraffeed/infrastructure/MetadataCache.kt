package net.qvarford.giraffeed.infrastructure

import jakarta.enterprise.context.ApplicationScoped

// Only cache the thing that requires network access to save memory.
// Use Sqlite as backing storage.
// Implement cache eviction on startup, we are not afraid that the cache overflows the disk in one day.
// Keep track of most-recently-used on get, and evict based on these criteria.
// We don't want to store MP4s this way, because we want to be able to use an efficient FileInputStream to stream over the internet, without loading the entire file into memory.
// Use row count as a proxy for when entries need to be evicted.
// Amount of feeds * Entries in each feed * cache count * row size (200 bytes seems like a reasonable upper bound)

interface MetadataCache {
    fun get(key: String): String?
    fun put(key: String, value: String)

    fun has(key: String) = get(key) != null
}