package ir.hadipoor.eviltower.monetization

import androidx.activity.ComponentActivity

data class BillingProduct(val sku: String, val title: String, val gems: Int)
object BillingCatalog {
    val gemsSmall = BillingProduct("gems_small", "کیسه جواهر کوچک", 20)
    val gemsMedium = BillingProduct("gems_medium", "کیسه جواهر بزرگ", 60)
    val gemsLarge = BillingProduct("gems_large", "صندوق جواهر", 200)
    val all = listOf(gemsSmall, gemsMedium, gemsLarge)
}
sealed interface PurchaseResult {
    data class Success(val product: BillingProduct, val token: String) : PurchaseResult
    data object Unavailable : PurchaseResult
    data object Canceled : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

/** Primary store seam: the production Bazaar build uses [BazaarBillingProvider] / Poolakey. */
interface BillingProvider {
    val storeName: String
    fun connect(onReady: (Boolean) -> Unit = {})
    fun purchase(activity: ComponentActivity, product: BillingProduct, onResult: (PurchaseResult) -> Unit)
    fun disconnect()
}
