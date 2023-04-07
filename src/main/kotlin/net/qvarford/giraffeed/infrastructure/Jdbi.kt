package net.qvarford.giraffeed.infrastructure

import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import java.time.Duration
import java.time.Instant
import jakarta.enterprise.context.ApplicationScoped
import org.sqlite.JDBC
import java.sql.DriverManager

@ApplicationScoped
class JdbiContext {
    private val jdbi = Jdbi.create("jdbc:sqlite:db.db")
        .installPlugin(KotlinPlugin())

    init {
        initializeSchema()
        evictSuitableEntries()
    }

    private fun initializeSchema() {
        jdbi.useHandle<Exception> {
            it.execute("""
                create table if not exists cache_entries (
                    cache_name text not null,
                    lru_timestamp int not null,
                    key text not null,
                    value text not null
                );
            """.trimIndent())
            it.execute("create unique index if not exists idx_cache_entries_name_key on cache_entries (cache_name, key);")
            it.execute("create index if not exists idx_cache_entries_timestamp on cache_entries (lru_timestamp)")
        }
    }

    private fun evictSuitableEntries() {
        jdbi.useHandleUnchecked  {
            it.createUpdate("delete from cache_entries where lru_timestamp < :earliest_timestamp")
                .bind("earliest_timestamp", Instant.now().minus(Duration.ofDays(2)).epochSecond)
                .execute()
        }
    }

    fun evictAllEntries() {
        jdbi.useHandleUnchecked  {
            it.createUpdate("delete from cache_entries")
                .execute()
        }
    }

    fun createCache(name: String): MetadataCache {
        return JdbiMetadataCache(jdbi, name)
    }
}

private class JdbiMetadataCache(private val jdbi: Jdbi, private val name: String) : MetadataCache {
    override fun get(key: String): String? {
        return jdbi.withHandleUnchecked {
            it.createUpdate("update cache_entries set lru_timestamp = unixepoch() where cache_name = :cache_name and key = :key")
                .bind("cache_name", name)
                .bind("key", key)
                .execute()
            it.select("select value from cache_entries where cache_name = :cache_name and key = :key")
                .bind("cache_name", name)
                .bind("key", key)
                .mapTo<String>()
                .firstOrNull()
        }
    }

    override fun put(key: String, value: String) {
        jdbi.useHandleUnchecked {
            it.createUpdate("insert into cache_entries (cache_name, lru_timestamp, key, value) values (:cache_name, unixepoch(), :key, :value)")
                .bind("cache_name", name)
                .bind("key", key)
                .bind("value", value)
                .execute()
        }
    }

}