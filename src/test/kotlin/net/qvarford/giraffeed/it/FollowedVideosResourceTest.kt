package net.qvarford.giraffeed.it

import au.com.origin.snapshots.serializers.ToStringSnapshotSerializer
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured
import net.qvarford.giraffeed.fake.InMemoryHttpClient
import net.qvarford.giraffeed.it.util.Resources
import net.qvarford.giraffeed.it.util.Verifier
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.net.http.HttpClient

@QuarkusTest
class FollowedVideosResourceTest {
    val map = mapOf(
        "https://api.twitch.tv/helix/channels/followed?user_id=29943195" to "twitch_followed_success.json",
        "https://api.twitch.tv/helix/videos?user_id=123456&period=day&type=archive&first=3" to "twitch_videos_success1.json",
        "https://api.twitch.tv/helix/videos?user_id=789012&period=day&type=archive&first=3" to "twitch_videos_success2.json"
    )

    @InjectMock
    lateinit var httpClient: HttpClient

    @BeforeEach
    fun setupHttpClient() {
        InMemoryHttpClient.create(httpClient, Resources.toResourceMap(map))
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
        private val verifier = Verifier(EnhancementResourceTest::class.java)

        @JvmStatic
        @AfterAll
        fun afterAll() {
            verifier.afterAll()
        }
    }
}