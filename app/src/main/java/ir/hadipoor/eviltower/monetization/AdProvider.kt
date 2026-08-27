package ir.hadipoor.eviltower.monetization

import android.app.Activity

/** Where a rewarded ad is offered. */
enum class AdPlacement {
    /** "ویدیو ببین و از همین طبقه ادامه بده" */
    CONTINUE_RUN,

    /** "سکه‌های این صعود را دو برابر کن" */
    DOUBLE_COINS,
}

/**
 * SDK-agnostic rewarded-ads abstraction.
 *
 * The shipping build uses [MockAdProvider] (no ad SDK compiled in) so the game has **zero**
 * Google Play Services dependencies — a requirement for Cafe Bazaar. Plugging in a real network
 * (Tapsell, Adivery, AdMob, ...) later only means writing another implementation of this
 * interface and handing it to `AdManager.provider`; no game code changes.
 */
interface AdProvider {
    val isEnabled: Boolean
    fun isRewardedReady(placement: AdPlacement): Boolean
    fun loadRewarded(placement: AdPlacement)

    /** @param onResult true when the player earned the reward. */
    fun showRewarded(activity: Activity, placement: AdPlacement, onResult: (Boolean) -> Unit)
}

/**
 * Placeholder implementation: pretends an ad was watched after a short delay.
 * Keeps the whole reward flow testable without shipping any tracking SDK.
 */
class MockAdProvider(override val isEnabled: Boolean = true) : AdProvider {

    private val loaded = mutableSetOf<AdPlacement>()

    override fun isRewardedReady(placement: AdPlacement): Boolean = isEnabled

    override fun loadRewarded(placement: AdPlacement) {
        loaded += placement
    }

    override fun showRewarded(activity: Activity, placement: AdPlacement, onResult: (Boolean) -> Unit) {
        if (!isEnabled) {
            onResult(false)
            return
        }
        // A real SDK would show a full-screen video here; the mock rewards immediately.
        activity.window.decorView.postDelayed({ onResult(true) }, 600L)
    }
}

/** Used when the player bought "حذف تبلیغات" or ads are disabled in a build. */
object DisabledAdProvider : AdProvider {
    override val isEnabled = false
    override fun isRewardedReady(placement: AdPlacement) = false
    override fun loadRewarded(placement: AdPlacement) = Unit
    override fun showRewarded(activity: Activity, placement: AdPlacement, onResult: (Boolean) -> Unit) =
        onResult(false)
}

/** Single swap-point for the whole app. */
object AdManager {
    var provider: AdProvider = MockAdProvider()
}
