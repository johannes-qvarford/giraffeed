package net.qvarford.giraffeed.fake

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.io.InputStream
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler

class InMemoryHttpClient {
    companion object {
        fun create(client: HttpClient, urlToContent: Map<String, InputStream>): HttpClient {
            Mockito.`when`(client.send(any(), any(BodyHandler::class.java)))
                .thenAnswer {
                    val request = it.arguments[0] as HttpRequest
                    val responseContent = urlToContent[request.uri().toString()] as InputStream
                    val response = Mockito.mock(HttpResponse::class.java) as HttpResponse<InputStream>

                    Mockito.`when`(response.statusCode())
                        .thenReturn(200)
                    Mockito.`when`(response.body())
                        .thenReturn(responseContent)
                    return@thenAnswer response
                }
            return client
        }
    }
}