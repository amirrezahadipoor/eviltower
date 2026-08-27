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

/**
 * Cafe Bazaar in-app billing, implemented with Bazaar's official SDK **Poolakey**
 * (`com.github.cafebazaar.Poolakey:poolakey`). This is the primary and only billing
 * implementation of the shipping build — Google Play Billing is deliberately absent.
 *
 * The RSA public key comes from the Bazaar developer console and is injected as
 * `BuildConfig.BAZAAR_RSA_KEY`; when it is empty the local security check is disabled so
 * debug builds still work.
 */
class BazaarBillingProvider(context: Context) : BillingProvider {

    override val storeName: String = "cafebazaar"

    private val appContext = context.applicationContext

    private val paymentConfiguration = PaymentConfiguration(
        localSecurityCheck = if (BuildConfig.BAZAAR_RSA_KEY.isNotBlank()) {
            SecurityCheck.Enable(rsaPublicKey = BuildConfig.BAZAAR_RSA_KEY)
        } else {
            SecurityCheck.Disable
        }
    )

    private val payment: Payment by lazy(LazyThreadSafetyMode.NONE) {
        Payment(context = appContext, config = paymentConfiguration)
    }

    private var connection: Connection? = null

    /** Bazaar must be installed for any purchase to be possible. */
    fun isBazaarInstalled(): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(BAZAAR_PACKAGE, 0)
        true
    }.getOrDefault(false)

    override fun connect(onReady: (Boolean) -> Unit) {
        if (!isBazaarInstalled()) {
            Log.i(TAG, "Cafe Bazaar is not installed - billing disabled")
            onReady(false)
            return
        }
        runCatching {
            connection = payment.connect {
                connectionSucceed { onReady(true) }
                connectionFailed { onReady(false) }
                disconnected { }
            }
        }.onFailure {
            Log.w(TAG, "connect failed: ${it.message}")
            onReady(false)
        }
    }

    override fun disconnect() {
        runCatching { connection?.disconnect() }
        connection = null
    }

    override fun isReady(): Boolean = connection?.getState() == ConnectionState.Connected

    override fun purchase(
        activity: ComponentActivity,
        product: BillingProduct,
        onResult: (PurchaseResult) -> Unit,
    ) {
        if (!isReady()) {
            onResult(PurchaseResult.Unavailable)
            return
        }
        runCatching {
            payment.purchaseProduct(
                registry = activity.activityResultRegistry,
                request = PurchaseRequest(productId = product.sku, payload = "eviltower"),
            ) {
                purchaseFlowBegan { }
                failedToBeginFlow { error ->
                    onResult(PurchaseResult.Failed(error.message ?: "failedToBeginFlow"))
                }
                purchaseSucceed { purchaseEntity ->
                    // Gem packs are consumable so they can be bought again.
                    if (product.gems > 0) {
                        runCatching {
                            payment.consumeProduct(purchaseEntity.purchaseToken) {
                                consumeSucceed { }
                                consumeFailed { }
                            }
                        }
                    }
                    onResult(PurchaseResult.Success(product, purchaseEntity.purchaseToken))
                }
                purchaseCanceled { onResult(PurchaseResult.Canceled) }
                purchaseFailed { error ->
                    onResult(PurchaseResult.Failed(error.message ?: "purchaseFailed"))
                }
            }
        }.onFailure {
            onResult(PurchaseResult.Failed(it.message ?: "unknown"))
        }
    }

    override fun queryOwnedProducts(onResult: (List<String>) -> Unit) {
        if (!isReady()) {
            onResult(emptyList())
            return
        }
        runCatching {
            payment.getPurchasedProducts {
                querySucceed { items -> onResult(items.map { it.productId }) }
                queryFailed { onResult(emptyList()) }
            }
        }.onFailure { onResult(emptyList()) }
    }

    companion object {
        private const val TAG = "BazaarBilling"
        const val BAZAAR_PACKAGE = "com.farsitel.bazaar"

        /** Opens the game's Bazaar page (used by the "امتیاز بده" / update prompts). */
        fun bazaarPageIntent(packageName: String) = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("bazaar://details?id=$packageName"),
        ).apply { setPackage(BAZAAR_PACKAGE) }

        fun hasPackage(context: Context, packageName: String): Boolean = runCatching {
            context.packageManager.getPackageInfo(packageName, 0) != null
        }.getOrElse { _: Throwable -> false }
    }
}

/** Small helper so callers can ask "is the store usable?" without touching PackageManager. */
fun Context.isCafeBazaarInstalled(): Boolean = runCatching {
    packageManager.getPackageInfo(BazaarBillingProvider.BAZAAR_PACKAGE, PackageManager.GET_ACTIVITIES)
    true
}.getOrDefault(false)
