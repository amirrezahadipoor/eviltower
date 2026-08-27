package ir.hadipoor.eviltower.monetization

import androidx.activity.ComponentActivity

/** A purchasable product (all optional — the full game is playable for free). */
data class BillingProduct(
    val sku: String,
    val persianName: String,
    val persianDescription: String,
    val gems: Int = 0,
    val removesAds: Boolean = false,
)

object BillingCatalog {
    val GEMS_SMALL = BillingProduct("gems_small", "کیسه جواهر کوچک", "۲۰ جواهر", gems = 20)
    val GEMS_MEDIUM = BillingProduct("gems_medium", "کیسه جواهر بزرگ", "۶۰ جواهر", gems = 60)
    val GEMS_LARGE = BillingProduct("gems_large", "صندوق جواهر", "۲۰۰ جواهر", gems = 200)
    val REMOVE_ADS = BillingProduct("remove_ads", "حذف تبلیغات", "همه ویدیوهای تبلیغاتی حذف می‌شوند", removesAds = true)

    val all = listOf(GEMS_SMALL, GEMS_MEDIUM, GEMS_LARGE, REMOVE_ADS)

    fun bySku(sku: String) = all.firstOrNull { it.sku == sku }
}

sealed interface PurchaseResult {
    data class Success(val product: BillingProduct, val purchaseToken: String) : PurchaseResult
    data object Canceled : PurchaseResult
    data class Failed(val message: String) : PurchaseResult
    data object Unavailable : PurchaseResult
}

/**
 * Store-agnostic billing abstraction.
 *
 * The **active implementation for this build is [BazaarBillingProvider]** (Cafe Bazaar / Poolakey).
 * Google Play Billing is intentionally *not* part of the project. A different store only needs a
 * new implementation of this interface.
 */
interface BillingProvider {
    val storeName: String
    fun connect(onReady: (Boolean) -> Unit = {})
    fun disconnect()
    fun isReady(): Boolean
    fun purchase(activity: ComponentActivity, product: BillingProduct, onResult: (PurchaseResult) -> Unit)
    fun queryOwnedProducts(onResult: (List<String>) -> Unit)
}

/** Fallback when Cafe Bazaar is not installed — purchases are simply unavailable. */
object UnavailableBillingProvider : BillingProvider {
    override val storeName = "none"
    override fun connect(onReady: (Boolean) -> Unit) = onReady(false)
    override fun disconnect() = Unit
    override fun isReady() = false
    override fun purchase(
        activity: ComponentActivity,
        product: BillingProduct,
        onResult: (PurchaseResult) -> Unit,
    ) = onResult(PurchaseResult.Unavailable)

    override fun queryOwnedProducts(onResult: (List<String>) -> Unit) = onResult(emptyList())
}

object BillingManager {
    var provider: BillingProvider = UnavailableBillingProvider
}
