package io.github.autocoupons.data.coupon

import io.github.autocoupons.domain.coupon.CouponFetchError
import io.github.autocoupons.domain.coupon.CouponFetchResult
import io.github.autocoupons.domain.coupon.CouponProvider
import io.github.autocoupons.domain.coupon.CouponSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.resume

/**
 * The assignment's coupon API: `getCoupons.php?pos=1` (Jabong) and `pos=2` (Myntra).
 *
 * Requests go over HTTPS: the host serves TLS even though the assignment lists an http:// URL,
 * so no cleartext exception is configured. As of 2026-10-24 both positions return HTTP 404;
 * the app reports that honestly instead of substituting data.
 */
class LegacyBuyhatkeCouponProvider(
    private val client: OkHttpClient,
    private val parser: CouponResponseParser = CouponResponseParser(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CouponProvider {

    override val source: CouponSource = CouponSource.LIVE_API

    override suspend fun fetchCoupons(retailerId: String): CouponFetchResult {
        val position = POSITIONS[retailerId]
            ?: return CouponFetchResult.Failure(CouponFetchError.UnsupportedRetailer(retailerId), source)

        val url = baseUrl.newBuilder()
            .addPathSegments("PickCoupon/FreshCoupon/getCoupons.php")
            .addQueryParameter("pos", position.toString())
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()

        val body = when (val response = execute(request)) {
            is HttpOutcome.Body -> response.text
            is HttpOutcome.Error -> return CouponFetchResult.Failure(response.error, source)
        }
        return when (val parsed = withContext(parseDispatcher) { parser.parse(body, retailerId, source) }) {
            is ParseResult.Parsed -> CouponFetchResult.Success(parsed.coupons, source)
            is ParseResult.Invalid -> CouponFetchResult.Failure(CouponFetchError.Malformed(parsed.reason), source)
        }
    }

    private sealed interface HttpOutcome {
        data class Body(val text: String) : HttpOutcome
        data class Error(val error: CouponFetchError) : HttpOutcome
    }

    /** Enqueues on OkHttp's dispatcher and cancels the call if the coroutine is cancelled. */
    private suspend fun execute(request: Request): HttpOutcome = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val error = if (e is InterruptedIOException) CouponFetchError.Timeout
                else CouponFetchError.Network(e.message ?: e.javaClass.simpleName)
                if (continuation.isActive) continuation.resume(HttpOutcome.Error(error))
            }

            override fun onResponse(call: Call, response: Response) {
                val outcome = response.use {
                    if (!it.isSuccessful) {
                        HttpOutcome.Error(CouponFetchError.Http(it.code))
                    } else {
                        try {
                            // peekBody bounds memory: an oversized payload is truncated and then fails parsing.
                            HttpOutcome.Body(it.peekBody(MAX_BODY_BYTES).string())
                        } catch (e: IOException) {
                            HttpOutcome.Error(CouponFetchError.Network(e.message ?: "Failed to read response"))
                        }
                    }
                }
                if (continuation.isActive) continuation.resume(outcome)
            }
        })
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://coupons.buyhatke.com/"
        private const val MAX_BODY_BYTES = 1L * 1024 * 1024

        /** `pos` values documented in the assignment. Jabong now redirects to Myntra. */
        val POSITIONS = mapOf("jabong" to 1, "myntra" to 2)
    }
}
