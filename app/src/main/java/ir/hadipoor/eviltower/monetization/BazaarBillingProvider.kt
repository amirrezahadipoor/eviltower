package ir.hadipoor.eviltower.monetization

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.ConnectionState
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.hadipoor.eviltower.BuildConfig

/** Cafe Bazaar's official Poolakey adapter. It is optional at runtime: core gameplay is offline. */
class BazaarBillingProvider(context: Context) : BillingProvider {
    override val storeName = "کافه‌بازار"
    private val app = context.applicationContext
    private val payment by lazy {
        Payment(context = app, config = PaymentConfiguration(
            localSecurityCheck = if (BuildConfig.BAZAAR_RSA_KEY.isBlank()) SecurityCheck.Disable else SecurityCheck.Enable(rsaPublicKey = BuildConfig.BAZAAR_RSA_KEY)
        ))
    }
    private var connection: Connection? = null
    fun isBazaarInstalled() = runCatching { app.packageManager.getPackageInfo(PACKAGE, 0); true }.getOrDefault(false)
    override fun connect(onReady: (Boolean) -> Unit) {
        if (!isBazaarInstalled()) return onReady(false)
        runCatching { connection = payment.connect { connectionSucceed { onReady(true) }; connectionFailed { onReady(false) }; disconnected {} } }
            .onFailure { Log.w("BazaarBilling", it.message.orEmpty()); onReady(false) }
    }
    override fun purchase(activity: ComponentActivity, product: BillingProduct, onResult: (PurchaseResult) -> Unit) {
        if (connection?.getState() != ConnectionState.Connected) return onResult(PurchaseResult.Unavailable)
        runCatching {
            payment.purchaseProduct(registry = activity.activityResultRegistry, request = PurchaseRequest(productId = product.sku, payload = "eviltower")) {
                purchaseSucceed { entity -> onResult(PurchaseResult.Success(product, entity.purchaseToken)) }
                purchaseCanceled { onResult(PurchaseResult.Canceled) }
                failedToBeginFlow { onResult(PurchaseResult.Failed(it.message.orEmpty())) }
                purchaseFailed { onResult(PurchaseResult.Failed(it.message.orEmpty())) }
            }
        }.onFailure { onResult(PurchaseResult.Failed(it.message.orEmpty())) }
    }
    override fun disconnect() { runCatching { connection?.disconnect() }; connection = null }
    companion object { const val PACKAGE = "com.farsitel.bazaar" }
}

fun Context.isCafeBazaarInstalled(): Boolean = runCatching { packageManager.getPackageInfo(BazaarBillingProvider.PACKAGE, PackageManager.GET_ACTIVITIES); true }.getOrDefault(false)
