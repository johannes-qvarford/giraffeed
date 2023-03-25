package net.qvarford.giraffeed.infrastructure

import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.NitterEntryUrl
import net.qvarford.giraffeed.domain.NitterVideoUrlFactory
import java.util.concurrent.ConcurrentHashMap

class CachingNitterVideoUrlFactory(private val delegate: NitterVideoUrlFactory) : NitterVideoUrlFactory {
    val map = ConcurrentHashMap<NitterEntryUrl, HlsUrl>()

    override fun lookup(id: NitterEntryUrl): HlsUrl {
        return map.computeIfAbsent(id) { delegate.lookup(id) }
    }
}