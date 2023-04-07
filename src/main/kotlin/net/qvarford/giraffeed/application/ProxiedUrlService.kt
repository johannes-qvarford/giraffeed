package net.qvarford.giraffeed.application

import jakarta.enterprise.context.ApplicationScoped
import net.qvarford.giraffeed.domain.FeedTypeFactory
import net.qvarford.giraffeed.domain.FeedUrl
import net.qvarford.giraffeed.domain.ProxyUrl
import net.qvarford.giraffeed.domain.SourceUrl

@ApplicationScoped
class ProxiedUrlService(val feedTypeFactory: FeedTypeFactory) {
    fun proxiedUrl(source: SourceUrl): ProxyUrl {
        val feedType = feedTypeFactory.ofSourceUrl(source)
        val resource = feedType.extractResource(source)
        val feedUrl = FeedUrl(feedType, resource)

        return feedUrl.proxied
    }
}