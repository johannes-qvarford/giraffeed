package net.qvarford.giraffeed.resources

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import net.qvarford.giraffeed.application.TwitchService
import net.qvarford.giraffeed.domain.TwitchUserAccessToken
import net.qvarford.giraffeed.infrastructure.FeedConverter
import java.io.InputStream
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/followed-videos")
class FollowedVideosResource(private val twitchService: TwitchService, private val feedConverter: FeedConverter) {
    @CheckedTemplate
    object Templates {
        @JvmStatic
        external fun index(): TemplateInstance
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun index(): TemplateInstance {
        return Templates.index()
    }

    @GET
    @Path("rss.xml")
    @Produces("application/atom+xml; charset=UTF-8")
    fun rss(@HeaderParam("Authorization") authorization: String): InputStream {
        val base64 = authorization.replace(Regex("Basic (.*)"), "$1")
        val username = String(Base64.getDecoder().decode(base64)).replace(Regex("(.*):.*"), "$1")
        val twitchUserAccessToken = TwitchUserAccessToken(username)
        return feedConverter.feedToXmlStream(twitchService.downloadLatestVideosFeed(twitchUserAccessToken))
    }
}