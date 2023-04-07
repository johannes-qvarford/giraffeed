package net.qvarford.giraffeed.it

import au.com.origin.snapshots.serializers.ToStringSnapshotSerializer
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured
import jakarta.inject.Inject
import net.qvarford.giraffeed.fake.InMemoryHttpClient
import net.qvarford.giraffeed.infrastructure.JdbiContext
import net.qvarford.giraffeed.it.util.Resources
import net.qvarford.giraffeed.it.util.Verifier
import org.junit.jupiter.api.*
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Path

@QuarkusTest
class SnapshotResourceTest {
    val map = mapOf(
        "https://www.reddit.com/r/AceAttorneyCirclejerk/hot.rss" to "libreddit_success.xml",
        "https://nitter.privacy.qvarford.net/slowbeef/rss" to "nitter_success.xml",
        "https://nitter.privacy.qvarford.net/InternetHippo/status/1635996454983548931#m" to "nitter_page_with_hls_video.html",
        "https://api.twitch.tv/helix/channels/followed?user_id=29943195" to "twitch_followed_success.json",
        "https://api.twitch.tv/helix/videos?user_id=123456&period=day&type=archive&first=3" to "twitch_videos_success1.json",
        "https://api.twitch.tv/helix/videos?user_id=789012&period=day&type=archive&first=3" to "twitch_videos_success2.json"
    )

    @InjectMock
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var context: JdbiContext

    @BeforeEach
    fun setupHttpClient() {
        InMemoryHttpClient.create(httpClient, Resources.toResourceMap(map))
    }

    @BeforeEach
    fun setupDb() {
        context.evictAllEntries()
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

    @Test
    fun twitchVideosAreMergedFromFollowedBroadcasters(testInfo: TestInfo) {
        val expect = verifier.expect(testInfo)
        val content = RestAssured.given()
            .auth().preemptive().basic("sample", "sample")
            .`when`().get("/followed-videos/atom.xml")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        expect.serializer(ToStringSnapshotSerializer::class.java).toMatchSnapshot(content)
    }

    companion object {
        private val verifier = Verifier(SnapshotResourceTest::class.java)

        @JvmStatic
        @AfterAll
        fun afterAll() {
            verifier.afterAll()
        }
    }
}