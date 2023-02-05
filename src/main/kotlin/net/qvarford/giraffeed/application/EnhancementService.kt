package net.qvarford.giraffeed.application

import net.qvarford.giraffeed.domain.*
import javax.enterprise.context.ApplicationScoped

interface EnhancementService {
    fun enhance(type: FeedType, resource: FeedResource): Feed
}

@ApplicationScoped
class FeedDownloaderDelegatingEnhancementService(
    private val downloader: FeedDownloader) : EnhancementService {
    override fun enhance(type: FeedType, resource: FeedResource): Feed {
        val url = FeedUrl(type = type, resource = resource)
        val feed = downloader.download(url)
        return type.enhance(feed)
    }
}