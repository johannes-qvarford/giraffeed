package net.qvarford.giraffeed.resources

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import net.qvarford.giraffeed.application.EnhancementService
import net.qvarford.giraffeed.domain.FeedResource
import net.qvarford.giraffeed.domain.FeedTypeFactory
import net.qvarford.giraffeed.infrastructure.FeedConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

@Path("/enhancement")
class EnhancementResource(private val service: EnhancementService, private val converter: FeedConverter, private val feedTypeFactory: FeedTypeFactory) {

    @GET
    @Path("{name}/{resource}")
    @Produces("application/atom+xml; charset=UTF-8")
    fun enhance(@PathParam("name") name: String, @PathParam("resource") resource: String): InputStream {
        try {
            val feed = service.enhance(feedTypeFactory.ofName(name), FeedResource(resource))
            return converter.feedToXmlStream(feed)
        } catch (e: Exception) {
            logger.error("Failed to enhance feed", e)
            throw e;
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(EnhancementResource::class.java)
    }
}