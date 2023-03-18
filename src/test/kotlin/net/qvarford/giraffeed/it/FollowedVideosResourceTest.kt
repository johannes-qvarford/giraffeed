package net.qvarford.giraffeed.it

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.SnapshotVerifier
import au.com.origin.snapshots.config.PropertyResolvingSnapshotConfig
import au.com.origin.snapshots.serializers.ToStringSnapshotSerializer
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import io.restassured.RestAssured
import net.qvarford.giraffeed.fake.InMemoryHttpClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.net.http.HttpClient

@QuarkusTest
class FollowedVideosResourceTest {
    @InjectMock
    lateinit var httpClient: HttpClient

    @BeforeEach
    fun setupHttpClient() {
        val resource = fun (s: String): InputStream {
            return this.javaClass.classLoader.getResourceAsStream(s)!!
        }

        val map = mapOf(
            "https://api.twitch.tv/helix/channels/followed?user_id=29943195" to resource("twitch_followed_success.json"),
            "https://api.twitch.tv/helix/videos?user_id=123456&period=day&type=archive&first=3" to resource("twitch_videos_success1.json"),
            "https://api.twitch.tv/helix/videos?user_id=789012&period=day&type=archive&first=3" to resource("twitch_videos_success2.json")
        )

        InMemoryHttpClient.create(httpClient, map)
    }

    @Test
    fun twitchVideosAreMergedFromFollowedBroadcasters() {
        val expect = Expect.of(FollowedVideosResourceTest.snapshotVerifier, FollowedVideosResourceTest::class.java.getMethod("twitchVideosAreMergedFromFollowedBroadcasters"))
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
        private var snapshotVerifier: SnapshotVerifier? = null

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            println(System.getProperty("java.class.path"))
            snapshotVerifier = SnapshotVerifier(PropertyResolvingSnapshotConfig(), FollowedVideosResourceTest::class.java)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            snapshotVerifier!!.validateSnapshots()
        }
    }
}