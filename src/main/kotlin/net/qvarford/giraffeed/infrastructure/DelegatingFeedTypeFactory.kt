package net.qvarford.giraffeed.infrastructure

import net.qvarford.giraffeed.domain.*
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class DelegatingFeedTypeFactory(nitterVideoUrlFactory: NitterVideoUrlFactory) : FeedTypeFactory {
    val feedTypes = arrayListOf(LibredditFeedType, NitterFeedType(nitterVideoUrlFactory))

    override fun ofName(name: String): FeedType {
        for (type in feedTypes) {
            if (type.name == name) {
                return type
            }
        }
        throw IllegalArgumentException("name")
    }

    override fun ofSourceUrl(url: SourceUrl): FeedType {
        for (type in feedTypes) {
            if (type.understandsSourceUrl(url)) {
                return type
            }
        }
        throw IllegalArgumentException("url")
    }

}