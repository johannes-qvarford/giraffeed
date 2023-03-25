package net.qvarford.giraffeed.resources

import net.qvarford.giraffeed.application.VideoConversionService
import net.qvarford.giraffeed.domain.HlsUrl
import net.qvarford.giraffeed.domain.Mp4
import java.net.URI
import java.security.MessageDigest
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response

// Example request : curl --verbose http://localhost:4567/hls-to-mp4/aHR0cHM6Ly9uaXR0ZXIucHJpdmFjeS5xdmFyZm9yZC5uZXQvdmlkZW8vQjE0NTMyOEI2MEFGNi9odHRwcyUzQSUyRiUyRnZpZGVvLnR3aW1nLmNvbSUyRmFtcGxpZnlfdmlkZW8lMkYxNjM1Nzc4NDkzNjg1NDIwMDM4JTJGcGwlMkYzckxxcFZXa2dCSnNTRWM0Lm0zdTglM0Z0YWclM0QxNiUyNmNvbnRhaW5lciUzRGZtcDQ=.mp4 >file.mp4

@Path("/hls-to-mp4")
class HlsToMp4Resource(private val service: VideoConversionService) {

    @Path("{segment}")
    @HEAD
    fun head(@PathParam("segment") hlsUrlBase64UrlSafeWithMp4Suffix: String): Response {
        val mp4 = getMp4(hlsUrlBase64UrlSafeWithMp4Suffix)

        return Response.ok()
            .header("Accept-Ranges", "none")
            .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
            .header(HttpHeaders.CONTENT_LENGTH, mp4.fileSize())
            .build()
    }

    @Path("{segment}")
    @GET
    fun index(@PathParam("segment") hlsUrlBase64UrlSafeWithMp4Suffix: String): Response {

        val mp4 = getMp4(hlsUrlBase64UrlSafeWithMp4Suffix)

        val baseName = baseName(hlsUrlBase64UrlSafeWithMp4Suffix)
        return Response.ok()
            .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
            .header(HttpHeaders.CONTENT_LENGTH, mp4.fileSize())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${shortenBaseName(baseName)}.mp4")
            .entity(mp4.stream())
            .build()
    }

    fun getMp4(hlsUrlBase64UrlSafeWithMp4Suffix: String) : Mp4 {
        val baseName = baseName(hlsUrlBase64UrlSafeWithMp4Suffix)

        val hlsUrl = HlsUrl(URI.create(String(Base64.getUrlDecoder().decode(baseName))))

        return service.convert(hlsUrl)
    }

    fun baseName(fileName: String): String {
        val tokens = fileName.split(Regex("\\.(?=[^.]+$)"))
        return tokens[0]
    }

    fun shortenBaseName(baseName: String): String {
        // Filenames can easily become over 200 characters long, which isn't a problem internally, but is annoying when downloading it.
        // As a last step, we therefor shorten it. Taking just the prefix or suffix has a high chance of creating conflicts,
        // so we just hash it. Subsequent requests will return the same hashed base name with the correct extension.

        val md = MessageDigest.getInstance("MD5")
        md.update(baseName.toByteArray())
        val digest = md.digest()
        return String(Base64.getUrlEncoder().encode(digest))
    }
}