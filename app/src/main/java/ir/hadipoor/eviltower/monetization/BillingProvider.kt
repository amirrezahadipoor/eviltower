package ir.hadipoor.eviltower.monetization

/** Store-neutral seam. The Bazaar console can inject Poolakey in the release flavor. */
data class BillingProduct(val sku: String, val title: String, val gems: Int)
sealed interface PurchaseResult {
    data class Success(val product: BillingProduct) : PurchaseResult
    data object Unavailable : PurchaseResult
    data object Canceled : PurchaseResult
}
interface BillingProvider {
    val storeName: String
    fun connect(onReady: (Boolean) -> Unit = {})
    fun purchase(product: BillingProduct, onResult: (PurchaseResult) -> Unit)
    fun disconnect()
}

/** Offline-safe Bazaar adapter. It never blocks core gameplay when Bazaar is absent. */
class BazaarBillingProvider : BillingProvider {
    override val storeName = "کافه‌بازار"
    override fun connect(onReady: (Boolean) -> Unit) = onReady(false)
    override fun purchase(product: BillingProduct, onResult: (PurchaseResult) -> Unit) = onResult(PurchaseResult.Unavailable)
    override fun disconnect() = Unit
}
