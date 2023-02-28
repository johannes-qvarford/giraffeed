package net.qvarford.giraffeed.infrastructure.quarkus

import java.net.http.HttpClient
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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

    @ApplicationScoped
    fun executorService(): ExecutorService {
        // We may be running on systems with a single core, but I want to be able to run downloads concurrently.
        // In the future, let's just use an executor that spawns virtual threads, or cut out the middleman
        // and use virtual threads directly.
        return Executors.newFixedThreadPool(8);
    }
}