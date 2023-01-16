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
class EnhancementResourceTest {
    @InjectMock
    lateinit var httpClient: HttpClient

    @BeforeEach
    fun setupHttpClient() {
        val resource = fun (s: String): InputStream {
            return this.javaClass.classLoader.getResourceAsStream(s)!!
        }

        val map = mapOf("https://reddit.com/r/AceAttorneyCirclejerk/hot.rss" to resource("libreddit_success.xml"))

        InMemoryHttpClient.create(httpClient, map)
    }

    @Test
    fun libredditFeedsAreFetchedFromReddit() {
        val expect = Expect.of(snapshotVerifier, EnhancementResourceTest::class.java.getMethod("libredditFeedsAreFetchedFromReddit"))
        val content = RestAssured.given()
            .`when`().get("/enhancement/libreddit/AceAttorneyCirclejerk")
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
            snapshotVerifier = SnapshotVerifier(PropertyResolvingSnapshotConfig(), EnhancementResourceTest::class.java)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            snapshotVerifier!!.validateSnapshots()
        }
    }
}