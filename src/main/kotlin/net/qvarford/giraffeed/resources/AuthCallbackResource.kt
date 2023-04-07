package net.qvarford.giraffeed.resources

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType


@Path("/auth/callback")
class AuthCallbackResource {
    @CheckedTemplate
    object Templates {
        @JvmStatic
        external fun index(): TemplateInstance
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun index(): TemplateInstance {
        return Templates.index();
    }
}