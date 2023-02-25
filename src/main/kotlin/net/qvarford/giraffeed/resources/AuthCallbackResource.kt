package net.qvarford.giraffeed.resources

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


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