package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlanPeriod {
    MONTHLY,
    ANNUAL
}

enum class BillingConnectionStatus {
    CONNECTING,
    READY,
    UNAVAILABLE,
    PRODUCTS_NOT_CONFIGURED
}

data class SubscriptionPlanOffer(
    val period: PlanPeriod,
    val productId: String,
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriodIso: String,
    val trialLabel: String?,
    val productDetails: ProductDetails
)

data class SubscriptionUiState(
    val connectionStatus: BillingConnectionStatus = BillingConnectionStatus.CONNECTING,
    val isProActive: Boolean = false,
    val isAutoRenewing: Boolean = false,
    val isPendingPurchase: Boolean = false,
    val activeProductId: String? = null,
    val monthlyOffer: SubscriptionPlanOffer? = null,
    val annualOffer: SubscriptionPlanOffer? = null,
    val statusMessage: String? = null,
    val isPurchasing: Boolean = false
)

class PlayBillingManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdate(billingResult, purchases)
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        connectAndSync()
    }

    fun connectAndSync() {
        if (billingClient.isReady) {
            scope.launch {
                querySubscriptionProducts()
                refreshActivePurchases()
            }
            return
        }

        _state.update {
            it.copy(
                connectionStatus = BillingConnectionStatus.CONNECTING,
                statusMessage = null
            )
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        querySubscriptionProducts()
                        refreshActivePurchases()
                    }
                } else {
                    val detail = billingResult.debugMessage.ifBlank { "code ${billingResult.responseCode}" }
                    _state.update {
                        it.copy(
                            connectionStatus = BillingConnectionStatus.UNAVAILABLE,
                            statusMessage = appContext.getString(R.string.billing_err_unavailable_fmt, detail)
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Automatic reconnection is enabled via enableAutoServiceReconnection().
            }
        })
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connectAndSync()
            return
        }
        scope.launch {
            _state.update { it.copy(statusMessage = appContext.getString(R.string.billing_msg_checking)) }
            refreshActivePurchases(userInitiated = true)
        }
    }

    fun launchPurchase(activity: Activity, offer: SubscriptionPlanOffer) {
        if (!billingClient.isReady) {
            _state.update { it.copy(statusMessage = appContext.getString(R.string.billing_msg_connecting)) }
            connectAndSync()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)
            .setOfferToken(offer.offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setIsOfferPersonalized(false)
            .build()

        _state.update { it.copy(isPurchasing = true, statusMessage = null) }
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            val detail = result.debugMessage.ifBlank { "code ${result.responseCode}" }
            _state.update {
                it.copy(
                    isPurchasing = false,
                    statusMessage = appContext.getString(R.string.billing_err_checkout_fmt, detail)
                )
            }
        }
    }

    fun clearStatusMessage() {
        _state.update { it.copy(statusMessage = null) }
    }

    private suspend fun querySubscriptionProducts() {
        val productList = ALL_SUBSCRIPTION_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params)
        }

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            val detail = result.billingResult.debugMessage.ifBlank { "code ${result.billingResult.responseCode}" }
            _state.update {
                it.copy(
                    connectionStatus = BillingConnectionStatus.UNAVAILABLE,
                    statusMessage = appContext.getString(R.string.billing_err_load_fmt, detail)
                )
            }
            return
        }

        val parsed = extractMonthlyAndAnnualOffers(result.productDetailsList.orEmpty())
        val hasAnyPlan = parsed.first != null || parsed.second != null
        _state.update {
            it.copy(
                connectionStatus = if (hasAnyPlan) {
                    BillingConnectionStatus.READY
                } else {
                    BillingConnectionStatus.PRODUCTS_NOT_CONFIGURED
                },
                monthlyOffer = parsed.first,
                annualOffer = parsed.second
            )
        }
    }

    private suspend fun refreshActivePurchases(userInitiated: Boolean = false) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val purchasesResult = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params)
        }

        if (purchasesResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (userInitiated) {
                _state.update {
                    it.copy(
                        statusMessage = appContext.getString(
                            R.string.billing_err_restore_fmt,
                            purchasesResult.billingResult.debugMessage
                        )
                    )
                }
            }
            return
        }

        val matchingPurchases = purchasesResult.purchasesList.filter { purchase ->
            purchase.products.any { it in ALL_SUBSCRIPTION_PRODUCT_IDS }
        }

        var proActive = false
        var autoRenewing = false
        var pending = false
        var activeProduct: String? = null

        for (purchase in matchingPurchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    acknowledgeIfNeeded(purchase)
                    proActive = true
                    autoRenewing = purchase.isAutoRenewing
                    activeProduct = purchase.products.firstOrNull()
                }
                Purchase.PurchaseState.PENDING -> {
                    pending = true
                }
            }
        }

        _state.update { state ->
            state.copy(
                isProActive = proActive,
                isAutoRenewing = autoRenewing,
                isPendingPurchase = pending,
                activeProductId = activeProduct,
                isPurchasing = false,
                statusMessage = when {
                    userInitiated && proActive -> appContext.getString(R.string.billing_msg_restored)
                    userInitiated && pending -> appContext.getString(R.string.billing_msg_pending)
                    userInitiated -> appContext.getString(R.string.billing_msg_none_found)
                    else -> state.statusMessage
                }
            )
        }
    }

    private fun handlePurchasesUpdate(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch {
                    val list = purchases.orEmpty()
                    var activated = false
                    var pending = false
                    for (purchase in list) {
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                acknowledgeIfNeeded(purchase)
                                activated = true
                            }
                            Purchase.PurchaseState.PENDING -> {
                                pending = true
                            }
                        }
                    }
                    refreshActivePurchases()
                    _state.update {
                        it.copy(
                            isPurchasing = false,
                            statusMessage = when {
                                activated -> appContext.getString(R.string.billing_msg_welcome)
                                pending -> appContext.getString(R.string.billing_msg_pending_unlock)
                                else -> it.statusMessage
                            }
                        )
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = appContext.getString(R.string.billing_msg_canceled)
                    )
                }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch {
                    refreshActivePurchases(userInitiated = true)
                }
            }

            else -> {
                val detail = billingResult.debugMessage.ifBlank { "code ${billingResult.responseCode}" }
                _state.update {
                    it.copy(
                        isPurchasing = false,
                        statusMessage = appContext.getString(R.string.billing_err_generic_fmt, detail)
                    )
                }
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            withContext(Dispatchers.IO) {
                billingClient.acknowledgePurchase(ackParams)
            }
        }
    }

    private fun extractMonthlyAndAnnualOffers(
        detailsList: List<ProductDetails>
    ): Pair<SubscriptionPlanOffer?, SubscriptionPlanOffer?> {
        var monthly: SubscriptionPlanOffer? = null
        var annual: SubscriptionPlanOffer? = null

        for (details in detailsList) {
            val offers = details.subscriptionOfferDetails.orEmpty()
            for (offer in offers) {
                val phases = offer.pricingPhases.pricingPhaseList
                val recurringPhase = phases.lastOrNull() ?: continue
                val period = classifyPeriod(
                    productId = details.productId,
                    basePlanId = offer.basePlanId,
                    billingPeriodIso = recurringPhase.billingPeriod
                ) ?: continue

                val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
                val trialLabel = trialPhase?.billingPeriod?.let { iso ->
                    appContext.getString(trialStringRes(iso))
                }

                val mapped = SubscriptionPlanOffer(
                    period = period,
                    productId = details.productId,
                    basePlanId = offer.basePlanId,
                    offerToken = offer.offerToken,
                    formattedPrice = recurringPhase.formattedPrice,
                    billingPeriodIso = recurringPhase.billingPeriod,
                    trialLabel = trialLabel,
                    productDetails = details
                )

                if (period == PlanPeriod.MONTHLY) {
                    if (monthly == null || (mapped.trialLabel != null && monthly.trialLabel == null)) {
                        monthly = mapped
                    }
                } else {
                    if (annual == null || (mapped.trialLabel != null && annual.trialLabel == null)) {
                        annual = mapped
                    }
                }
            }
        }
        return monthly to annual
    }

    fun release() {
        scope.cancel()
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        const val PRODUCT_ID_PRO = "gps_scanner_pro"
        const val PRODUCT_ID_PRO_MONTHLY = "gps_scanner_pro_monthly"
        const val PRODUCT_ID_PRO_ANNUAL = "gps_scanner_pro_annual"

        val ALL_SUBSCRIPTION_PRODUCT_IDS = listOf(
            PRODUCT_ID_PRO,
            PRODUCT_ID_PRO_MONTHLY,
            PRODUCT_ID_PRO_ANNUAL
        )

        fun classifyPeriod(productId: String, basePlanId: String, billingPeriodIso: String): PlanPeriod? {
            val pid = productId.lowercase()
            val bid = basePlanId.lowercase()
            val iso = billingPeriodIso.uppercase()
            return when {
                iso == "P1M" || bid.contains("month") || pid.contains("month") -> PlanPeriod.MONTHLY
                iso == "P1Y" || bid.contains("year") || bid.contains("annual") ||
                    pid.contains("year") || pid.contains("annual") -> PlanPeriod.ANNUAL
                else -> null
            }
        }

        fun trialStringRes(iso: String): Int = when (iso.uppercase()) {
            "P3D" -> R.string.trial_3d
            "P7D", "P1W" -> R.string.trial_7d
            "P14D", "P2W" -> R.string.trial_14d
            "P1M" -> R.string.trial_1m
            else -> R.string.trial_generic
        }
    }
}
