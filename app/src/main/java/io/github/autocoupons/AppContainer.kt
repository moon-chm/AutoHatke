package io.github.autocoupons

import io.github.autocoupons.data.checkout.RetailerRegistry
import io.github.autocoupons.data.coupon.DefaultCouponRepository
import io.github.autocoupons.data.coupon.DemoCouponProvider
import io.github.autocoupons.data.coupon.LegacyBuyhatkeCouponProvider
import io.github.autocoupons.domain.coupon.CouponApplicationCoordinator
import io.github.autocoupons.domain.coupon.CouponRepository
import io.github.autocoupons.domain.coupon.DefaultCouponRankingEngine
import io.github.autocoupons.feature.auth.DemoSessionStore
import io.github.autocoupons.feature.home.LastSessionStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency graph. The app is small enough that a DI framework would add more weight
 * than it removes. Everything here is application-scoped and holds no Activity or View.
 */
class AppContainer {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            // One attempt per user action: no silent retries that cost battery or hide failures.
            .retryOnConnectionFailure(false)
            .build()
    }

    val couponRepository: CouponRepository by lazy {
        DefaultCouponRepository(
            listOf(LegacyBuyhatkeCouponProvider(okHttpClient), DemoCouponProvider()),
        )
    }

    val coordinator: CouponApplicationCoordinator by lazy {
        CouponApplicationCoordinator(couponRepository, DefaultCouponRankingEngine())
    }

    val retailerRegistry = RetailerRegistry()
    val demoSessionStore = DemoSessionStore()
    val lastSessionStore = LastSessionStore()
}
