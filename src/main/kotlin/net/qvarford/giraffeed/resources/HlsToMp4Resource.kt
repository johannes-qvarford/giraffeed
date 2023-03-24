package net.qvarford.giraffeed.resources

import net.qvarford.giraffeed.application.VideoConversionService
import net.qvarford.giraffeed.domain.HlsUrl
import java.net.URI
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response

@Path("/hls-to-mp4")
class HlsToMp4Resource(private val service: VideoConversionService) {

    @Path("{hlsUrlBase64UrlSafeWithMp4Suffix}")
    @GET
    fun index(@PathParam("hlsUrlBase64UrlSafeWithMp4Suffix") segment: String, @HeaderParam("Range") range: String?): Response {
        if (range != null) {
            throw NotSupportedException("Don't support range queries yet")
        }

        val tokens = segment.split(Regex("\\.(?=[^\\.]+$)"))
        val baseName = tokens[0]

        val hlsUrl = HlsUrl(URI.create(String(Base64.getUrlDecoder().decode(baseName))))

        val mp4 = service.convert(hlsUrl)

        return Response.ok()
            .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$segment")
            .entity(mp4.allBytes())
            .build()
    }
}