package com.program.braintrainer.chess.model.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class BillingClientManager(
    private val context: Context,
    private val externalScope: CoroutineScope, // Koristi scope spolja (npr. viewModelScope)
    private val onPurchaseSuccess: () -> Unit
) {
    // Listener za sve promene u vezi sa kupovinama
    private val purchasesUpdatedListener = object : PurchasesUpdatedListener {
        override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                externalScope.launch {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.d("BillingClient", "User canceled the purchase flow.")
            } else {
                Log.w("BillingClient", "Purchase update error: ${billingResult.debugMessage}")
            }
        }
    }

    // Inicijalizacija BillingClient-a sa automatskim ponovnim povezivanjem
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        // ISPRAVKA: Eksplicitno omogućavanje pending purchases za jednokratne proizvode
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection() // Preporučeni način za rukovanje prekidima veze
        .build()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    init {
        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        if (billingClient.isReady) {
            Log.d("BillingClient", "BillingClient is already connected.")
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingClient", "Billing Client Connected.")
                    externalScope.launch {
                        queryProductDetails()
                        queryExistingPurchases()
                    }
                } else {
                    Log.e("BillingClient", "Connection failed: ${billingResult.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                // Sa enableAutoServiceReconnection(), biblioteka sama pokušava da se poveže.
                // Ručno ponovno povezivanje ovde više nije neophodno.
                Log.w("BillingClient", "Billing Client Disconnected. Auto-reconnect is enabled.")
            }
        })
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_upgrade") // <-- Vaš ID proizvoda
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

        val result = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params.build())
        }

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
            if (result.productDetailsList.isNullOrEmpty()){
                Log.w("BillingClient", "No product details found for 'premium_upgrade'. Check your Product ID in Google Play Console.")
            }
        } else {
            Log.e("BillingClient", "Failed to query product details: ${result.billingResult.debugMessage}")
        }
    }

    suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params)
        }

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.purchasesList.forEach { purchase ->
                handlePurchase(purchase)
            }
        } else {
            Log.e("BillingClient", "Failed to query existing purchases: ${result.billingResult.debugMessage}")
        }
    }

    // AŽURIRANO: Funkcija je sada 'suspend' da bi se sačekali detalji o proizvodu.
    // Ovo rešava problem "race condition" gde se kupovina pokreće pre nego što su podaci učitani.
    suspend fun launchPurchaseFlow(activity: Activity) {
        // Sačekaj da detalji o proizvodu budu dostupni ako već nisu.
        // Koristi se timeout da se izbegne beskonačno čekanje.
        val productDetails = try {
            _productDetails.value ?: withTimeout(5000L) { // 5 sekundi timeout
                _productDetails.first { it != null }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("BillingClient", "Isteklo je vreme čekanja na detalje o proizvodu.")
            null
        }

        // Ako detalji i dalje nisu dostupni, prikaži grešku i pokušaj ponovo da ih učitaš kao rezervnu opciju.
        if (productDetails == null) {
            Log.e("BillingClient", "Pokretanje kupovine nije uspelo: Detalji o proizvodu nisu učitani.")
            if (billingClient.isReady) {
                Log.d("BillingClient", "Detalji o proizvodu nisu dostupni, pokrećem ponovno učitavanje...")
                queryProductDetails()
            }
            return
        }

        val offerToken = productDetails.oneTimePurchaseOfferDetails?.offerToken ?: ""
        if (offerToken.isEmpty()) {
            Log.e("BillingClient", "Pokretanje kupovine nije uspelo: Nije pronađen validan offer token za proizvod.")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        // launchBillingFlow se mora pozvati na glavnoj (UI) niti.
        withContext(Dispatchers.Main) {
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val ackResult = withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                }
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingClient", "Purchase Acknowledged.")
                    onPurchaseSuccess()
                } else {
                    Log.e("BillingClient", "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                }
            } else {
                Log.d("BillingClient", "Purchase already acknowledged.")
                onPurchaseSuccess()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d("BillingClient", "Purchase is pending.")
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            Log.d("BillingClient", "Ending connection.")
            billingClient.endConnection()
        }
    }
}
