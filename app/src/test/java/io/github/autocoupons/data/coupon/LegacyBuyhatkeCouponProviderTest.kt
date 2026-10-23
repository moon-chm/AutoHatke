package io.github.autocoupons.data.coupon

import io.github.autocoupons.domain.coupon.CouponFetchError
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class LegacyBuyhatkeCouponProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: LegacyBuyhatkeCouponProvider

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        val client = OkHttpClient.Builder().readTimeout(500, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
        provider = LegacyBuyhatkeCouponProvider(client, baseUrl = server.url("/"), parseDispatcher = Dispatchers.Unconfined)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `requests the documented path and pos for each retailer`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))
        server.enqueue(MockResponse().setBody("[]"))
        provider.fetchCoupons("myntra")
        provider.fetchCoupons("jabong")
        assertEquals("/PickCoupon/FreshCoupon/getCoupons.php?pos=2", server.takeRequest().path)
        assertEquals("/PickCoupon/FreshCoupon/getCoupons.php?pos=1", server.takeRequest().path)
    }

    @Test fun `successful payload is parsed and labelled live`() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"code":"LIVE1"},{"code":"LIVE2"}]"""))
        val result = provider.fetchCoupons("myntra") as CouponFetchResult.Success
        assertEquals(listOf("LIVE1", "LIVE2"), result.coupons.map { it.code })
        assertEquals(CouponSource.LIVE_API, result.source)
    }

    @Test fun `404 with the real nginx body maps to Http 404`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody(CouponResponseParserTest.realNotFoundBody))
        assertEquals(CouponFetchResult.Failure(CouponFetchError.Http(404), CouponSource.LIVE_API), provider.fetchCoupons("myntra"))
    }

    @Test fun `200 with html maps to malformed`() = runBlocking {
        server.enqueue(MockResponse().setBody("<html>maintenance</html>"))
        val error = (provider.fetchCoupons("myntra") as CouponFetchResult.Failure).error
        assertTrue(error is CouponFetchError.Malformed)
    }

    @Test fun `slow server maps to timeout`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]").setHeadersDelay(2, TimeUnit.SECONDS))
        assertEquals(CouponFetchError.Timeout, (provider.fetchCoupons("myntra") as CouponFetchResult.Failure).error)
    }

    @Test fun `dropped connection maps to network error`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val error = (provider.fetchCoupons("myntra") as CouponFetchResult.Failure).error
        assertTrue(error.toString(), error is CouponFetchError.Network || error is CouponFetchError.Timeout)
    }

    @Test fun `unknown retailer makes no request`() = runBlocking {
        assertEquals(CouponFetchError.UnsupportedRetailer("demo-store"), (provider.fetchCoupons("demo-store") as CouponFetchResult.Failure).error)
        assertEquals(0, server.requestCount)
    }

    @Test fun `repository never mixes sources`() = runBlocking {
        val repo = DefaultCouponRepository(listOf(provider, DemoCouponProvider()))
        val demo = repo.fetchCoupons("demo-store", CouponSource.DEMO_FIXTURE) as CouponFetchResult.Success
        assertTrue(demo.coupons.all { it.source == CouponSource.DEMO_FIXTURE })
        val demoForMyntra = repo.fetchCoupons("myntra", CouponSource.DEMO_FIXTURE)
        assertTrue(demoForMyntra is CouponFetchResult.Failure)
        assertEquals(0, server.requestCount)
    }
}
