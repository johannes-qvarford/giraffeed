package net.qvarford.giraffeed.infrastructure.quarkus

import java.net.http.HttpClient
import javax.enterprise.context.ApplicationScoped
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

@ApplicationScoped
class GiraffeedConfiguration {
    @ApplicationScoped
    fun documentBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newDocumentBuilder()
    }

    @ApplicationScoped
    fun httpClient(): HttpClient {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build()
    }
}