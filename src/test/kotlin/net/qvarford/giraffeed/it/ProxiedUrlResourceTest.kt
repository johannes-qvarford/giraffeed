package net.qvarford.giraffeed.it

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@QuarkusTest
internal class ProxiedUrlResourceTest {
    @ParameterizedTest
    @MethodSource
    fun feedsCanBeProxied(sourceUrl: String, expectedProxyUrl: String) {
        RestAssured.given()
            .queryParam("source-url", sourceUrl)
            .`when`()
            .get("/proxied-url")
            .then()
            .statusCode(200)
            .body("proxy_url", equalTo(expectedProxyUrl))
    }

    companion object {
        @JvmStatic
        fun feedsCanBeProxied(): Stream<Arguments> {
            val expectedLibredditProxyUrl = "https://giraffeed.privacy.qvarford.net/enhancement/libreddit/example"
            val expectedNitterProxyUrl = "https://giraffeed.privacy.qvarford.net/enhancement/nitter/example"
            return Stream.of(
                arguments("https://libreddit.privacy.qvarford.net/r/example", expectedLibredditProxyUrl),
                arguments("https://reddit.com/r/example", expectedLibredditProxyUrl),
                arguments("https://www.reddit.com/r/example", expectedLibredditProxyUrl),
                arguments("https://reddit.com/r/example/hot.rss", expectedLibredditProxyUrl),

                arguments("https://nitter.privacy.qvarford.net/example", expectedNitterProxyUrl),
                arguments("https://twitter.com/example", expectedNitterProxyUrl),
            )
        }
    }
}