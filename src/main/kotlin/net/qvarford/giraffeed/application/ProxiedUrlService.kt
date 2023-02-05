package net.qvarford.giraffeed.application

import net.qvarford.giraffeed.domain.ProxyUrl
import net.qvarford.giraffeed.domain.SourceUrl
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ProxiedUrlService() {
    fun proxiedUrl(source: SourceUrl): ProxyUrl {
        return source.feedUrl.proxied
    }
}