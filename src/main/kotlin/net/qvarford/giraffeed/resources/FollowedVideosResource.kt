package net.qvarford.giraffeed.resources

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import net.qvarford.giraffeed.application.TwitchService
import net.qvarford.giraffeed.domain.TwitchUserAccessToken
import net.qvarford.giraffeed.infrastructure.FeedConverter
import java.io.InputStream
import java.util.*
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

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
    fun rss(@HeaderParam("Authorization") authorization: String?): InputStream {
        val base64 = authorization!!.replace(Regex("Basic (.*)"), "$1")
        val username = String(Base64.getDecoder().decode(base64)).replace(Regex("(.*):.*"), "$1")
        val twitchUserAccessToken = TwitchUserAccessToken(username)
        return feedConverter.feedToXmlStream(twitchService.downloadLatestVideosFeed(twitchUserAccessToken))
    }

    @GET
    @Path("atom.xml")
    @Produces("application/atom+xml; charset=UTF-8")
    fun atom(@HeaderParam("Authorization") authorization: String?): InputStream {
        return rss(authorization);
    }
}