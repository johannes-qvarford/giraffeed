package net.qvarford.giraffeed.fake

import org.codehaus.plexus.util.StringInputStream
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class InMemoryHttpClient {
    companion object {
        fun create(client: HttpClient, urlToContent: Map<String, InputStream>): HttpClient {
            Mockito.`when`(client.send(any(), any(BodyHandler::class.java)))
                .thenAnswer {
                    val request = it.arguments[0] as HttpRequest
                    val responseContent = urlToContent[request.uri().toString()]
                        ?: fallbackToRealResource(request.uri()) //throw RuntimeException("No content mapping for: ${request.uri()}")
                    val response = Mockito.mock(HttpResponse::class.java) as HttpResponse<*>

                    Mockito.`when`(response.statusCode())
                        .thenReturn(200)
                    Mockito.`when`(response.body())
                        .thenReturn(responseContent)
                    return@thenAnswer response
                }
            return client
        }

        fun fallbackToRealResource(uri: URI): InputStream {
            try {
                val httpClient = HttpClient.newHttpClient()
                val request = HttpRequest.newBuilder(uri).build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
                if (response.statusCode() != 200) {
                    throw RuntimeException("No content mapping for: $uri , failed to fetch the real resource")
                }

                val content = String(response.body().readAllBytes())

                val filename = "${Random().nextInt()}.json"
                println("\"$uri\" to \"$filename\"")
                Files.write(Path.of(filename), content.toByteArray())

                return ByteArrayInputStream(content.toByteArray())
            } catch (e: Exception) {
                throw RuntimeException("Something went wrong", e)
            }
        }
    }
}