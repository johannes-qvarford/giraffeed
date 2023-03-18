package net.qvarford.giraffeed.domain

// We can use this to download high-quality images that requires reading the HTML source.
// The caller can be vigilant, and not use this if the entry was published more than 25 hours ago, because it has
// most likely already been picked up by then, and the cache will get cleared at regular intervals, so we don't want to refill the cache.

interface MetadataCache<V> {
    fun get(key: String): V
    fun put(key: String, value: V)
}

interface MetadataCacheProvider {
    fun <T> get(cls: Class<T>): MetadataCache<T>
}