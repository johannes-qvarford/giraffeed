package net.qvarford.giraffeed.it.util

import java.io.InputStream
import java.util.stream.Collectors

class Resources {
    companion object {
        fun <K> toResourceMap(map: Map<K, String>): Map<K, InputStream> {
            return map.entries.stream().collect(Collectors.toMap({it.key}, {resource(it.value)}))
        }

        private fun resource(s: String): InputStream {
            return Resources::class.java.classLoader.getResourceAsStream(s) ?: throw RuntimeException("Could not find resource: $s")
        }
    }
}

