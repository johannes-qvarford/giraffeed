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

        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/bb8vaf/yall_fuckers_better_jerk_it_good/.json" to "libreddit/-47181450.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10036qp/haapynewyear/.json" to "libreddit/-1627760256.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10bu04x/what_a_cool_guy_hope_he_beats_the_nickel_samurai/.json" to "libreddit/-1755671897.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10bivnz/the_ratings_rajah/.json" to "libreddit/-807178439.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10az16h/so_hot/.json" to "libreddit/751846212.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10b0l3e/wow_what_a_nicelooking_man_surely_he_will_not/.json" to "libreddit/-347894873.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10arx3g/someone_poisoned_kristophs_pissklavier_finds_this/.json" to "libreddit/433061365.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10b2qdw/guys_how_do_i_pass_this_case_i_know_its_something/.json" to "libreddit/582730903.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10atxnv/carcinisation_or_carcinization_is_an_example_of/.json" to "libreddit/-536036016.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10ayn48/guys_i_fixed_it/.json" to "libreddit/-1778127460.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10azbeg/damn_girl/.json" to "libreddit/-56491573.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10ac0mi/your_honor_his_eyebrow_hair_shifted_36_nanometres/.json" to "libreddit/36794303.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10awkti/apoollo_juice_test_shimeji/.json" to "libreddit/308392455.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10a4xrf/_/.json" to "libreddit/1798751771.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10a04iz/the_worst_meme_i_have_ever_made/.json" to "libreddit/-1645395091.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109urvd/the_judge_be_like/.json" to "libreddit/-1177810080.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10a7b7o/real/.json" to "libreddit/1511579698.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/10a64nd/describe_a_character_in_your_favorite_case_as_a/.json" to "libreddit/1168789277.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109wjss/major_aai2_spoilers_but_moozilla_tho/.json" to "libreddit/-1965196907.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109uyob/whos_gonna_win/.json" to "libreddit/1388344884.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109dml2/ties_are_the_danganronpa_hair_of_ace_attorney/.json" to "libreddit/-1298337909.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109q8dx/both_have_been_in_a_murder_trial_too/.json" to "libreddit/-2038895083.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109s77o/spoilers_for_the_wrong_trowsers/.json" to "libreddit/2006019535.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109gv8j/and_we_thought_pisstoph_was_bad_enough/.json" to "libreddit/243368678.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/1092zix/valentines_be_like/.json" to "libreddit/-651421054.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/1099cdu/ok_then_he_likes_to_be_stepped_on_apparently/.json" to "libreddit/-406580547.json",
        "https://www.reddit.com/r/AceAttorneyCirclejerk/comments/109eo96/okay_something_thats_been_bothering_me_for_a/.json" to "libreddit/47401351.json",

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