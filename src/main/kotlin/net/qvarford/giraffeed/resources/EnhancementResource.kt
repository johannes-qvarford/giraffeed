package net.qvarford.giraffeed.resources

import net.qvarford.giraffeed.application.EnhancementService
import net.qvarford.giraffeed.domain.FeedResource
import net.qvarford.giraffeed.domain.FeedTypeFactory
import net.qvarford.giraffeed.infrastructure.FeedConverter
import java.io.InputStream
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

@Path("/enhancement")
class EnhancementResource(private val service: EnhancementService, private val converter: FeedConverter, private val feedTypeFactory: FeedTypeFactory) {

    @GET
    @Path("{name}/{resource}")
    @Produces("application/atom+xml; charset=UTF-8")
    fun enhance(@PathParam("name") name: String, @PathParam("resource") resource: String): InputStream {
        val feed = service.enhance(feedTypeFactory.ofName(name), FeedResource(resource))
        return converter.feedToXmlStream(feed)
    }
}