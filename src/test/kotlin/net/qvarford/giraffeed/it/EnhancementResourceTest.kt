package net.qvarford.giraffeed.it

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured
import net.qvarford.giraffeed.fake.InMemoryHttpClient
import net.qvarford.giraffeed.it.util.Resources
import net.qvarford.giraffeed.it.util.Verifier
import org.junit.jupiter.api.*
import java.net.http.HttpClient

@QuarkusTest
class EnhancementResourceTest {
    val map = mapOf(
        "https://www.reddit.com/r/AceAttorneyCirclejerk/hot.rss" to "libreddit_success.xml",
        "https://nitter.privacy.qvarford.net/slowbeef/rss" to "nitter_success.xml",
        "https://nitter.privacy.qvarford.net/InternetHippo/status/1635996454983548931#m" to "nitter_page_with_hls_video.html"
    )

    @InjectMock
    lateinit var httpClient: HttpClient

    @BeforeEach
    fun setupHttpClient() {
        InMemoryHttpClient.create(httpClient, Resources.toResourceMap(map))
    }

    @Test
    fun libredditFeedsAreFetchedFromReddit(testInfo: TestInfo) {
        val expect = verifier.expect(testInfo)
        val content = RestAssured.given()
            .`when`().get("/enhancement/libreddit/AceAttorneyCirclejerk")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        expect.toMatchSnapshot(content)
    }

    @Test
    fun nitterFeedsAreFetchedFromNitter(testInfo: TestInfo) {
        val expect = verifier.expect(testInfo)
        val content = RestAssured.given()
            .`when`().get("/enhancement/nitter/slowbeef")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        expect.toMatchSnapshot(content)
    }

    companion object {
        private val verifier = Verifier(EnhancementResourceTest::class.java)

        @JvmStatic
        @AfterAll
        fun afterAll() {
            verifier.afterAll()
        }
    }
}