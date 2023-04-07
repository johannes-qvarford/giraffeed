package net.qvarford.giraffeed.resources

import net.qvarford.giraffeed.application.ProxiedUrlService
import net.qvarford.giraffeed.domain.SourceUrl
import jakarta.ws.rs.Path
import jakarta.ws.rs.GET
import jakarta.ws.rs.QueryParam
import java.net.URI

@Path("/proxied-url")
class ProxiedUrlResource(private val service: ProxiedUrlService) {

    @GET
    fun index(@QueryParam("source-url") sourceUrl: String): ProxiedUrlResponse {
        val url = SourceUrl(URI.create(sourceUrl))
        return ProxiedUrlResponse(service.proxiedUrl(url).value.toString())
    }
}

data class ProxiedUrlResponse(val proxyUrl: String)