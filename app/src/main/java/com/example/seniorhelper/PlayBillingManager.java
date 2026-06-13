package com.example.seniorhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

public final class PlayBillingManager implements PurchasesUpdatedListener {
    public static final String SUBSCRIPTION_PRODUCT_ID = "premium_monthly";
    public static final String MONTHLY_BASE_PLAN_ID = "monthly";
    public static final String LIFETIME_PRODUCT_ID = "premium_lifetime";

    public enum PremiumPlan {
        NONE,
        MONTHLY,
        LIFETIME
    }

    public interface Listener {
        void onPremiumStatusChanged(PremiumPlan plan, boolean firstCheck);

        void onMonthlySubscriptionStateChanged(
                boolean active,
                boolean autoRenewing,
                long purchaseTimeMillis
        );

        void onBillingProductChanged(boolean available);

        void onBillingMessage(String message);
    }

    private final Context appContext;
    private final Listener listener;
    private BillingClient billingClient;
    private ProductDetails monthlyProduct;
    private ProductDetails lifetimeProduct;
    private String monthlyOfferToken;
    private String lifetimeOfferToken;
    private String monthlyPrice = "¥500 / 月";
    private String lifetimePrice = "¥3,000";
    private boolean firstPurchaseQueryCompleted = false;
    private boolean productQueryFinished = false;
    private int productQueryGeneration = 0;

    public PlayBillingManager(Context context, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    public void start() {
        if (billingClient != null) {
            return;
        }

        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .enableAutoServiceReconnection()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                    refreshPurchases(false);
                } else {
                    productQueryFinished = true;
                    listener.onBillingProductChanged(false);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // BillingClient reconnects automatically.
            }
        });
    }

    public boolean isMonthlyAvailable() {
        return monthlyProduct != null && monthlyOfferToken != null;
    }

    public boolean isLifetimeAvailable() {
        return lifetimeProduct != null;
    }

    public boolean isAnyProductAvailable() {
        return isMonthlyAvailable() || isLifetimeAvailable();
    }

    public String getMonthlyPrice() {
        return monthlyPrice;
    }

    public String getLifetimePrice() {
        return lifetimePrice;
    }

    public boolean isProductQueryFinished() {
        return productQueryFinished;
    }

    public void launchMonthlyPurchase(Activity activity) {
        if (!isMonthlyAvailable()) {
            handleUnavailableProduct("月額プラン");
            return;
        }
        launchPurchase(activity, monthlyProduct, monthlyOfferToken);
    }

    public void launchLifetimePurchase(Activity activity) {
        if (!isLifetimeAvailable()) {
            handleUnavailableProduct("買い切りプラン");
            return;
        }
        launchPurchase(activity, lifetimeProduct, lifetimeOfferToken);
    }

    public void refreshPurchases(boolean userInitiated) {
        if (!isReady()) {
            if (userInitiated) {
                listener.onBillingMessage("Google Playに接続しています。少し待ってからもう一度お試しください。");
            }
            start();
            return;
        }

        PurchaseRefresh refresh = new PurchaseRefresh(userInitiated);
        queryPurchases(BillingClient.ProductType.SUBS, refresh);
        queryPurchases(BillingClient.ProductType.INAPP, refresh);
    }

    public void openManageSubscription(Activity activity) {
        Uri uri = Uri.parse(
                "https://play.google.com/store/account/subscriptions?sku="
                        + SUBSCRIPTION_PRODUCT_ID
                        + "&package="
                        + activity.getPackageName()
        );
        activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    public void endConnection() {
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            PremiumPlan purchasedPlan = PremiumPlan.NONE;
            boolean pending = false;
            boolean monthlyActive = false;
            boolean monthlyAutoRenewing = false;
            long monthlyPurchaseTime = 0L;
            for (Purchase purchase : purchases) {
                PremiumPlan plan = planForPurchase(purchase);
                if (plan == PremiumPlan.NONE) {
                    continue;
                }
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    if (plan == PremiumPlan.LIFETIME) {
                        purchasedPlan = PremiumPlan.LIFETIME;
                    } else if (purchasedPlan == PremiumPlan.NONE) {
                        purchasedPlan = PremiumPlan.MONTHLY;
                    }
                    if (plan == PremiumPlan.MONTHLY) {
                        monthlyActive = true;
                        monthlyAutoRenewing = purchase.isAutoRenewing();
                        monthlyPurchaseTime = purchase.getPurchaseTime();
                    }
                    acknowledgeIfNeeded(purchase);
                } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    pending = true;
                }
            }
            if (purchasedPlan != PremiumPlan.NONE) {
                listener.onPremiumStatusChanged(purchasedPlan, false);
                listener.onMonthlySubscriptionStateChanged(
                        monthlyActive,
                        monthlyAutoRenewing,
                        monthlyPurchaseTime
                );
                listener.onBillingMessage("プレミアム会員の登録が完了しました。");
            } else if (pending) {
                listener.onBillingMessage("お支払いは確認中です。完了後にプレミアムが有効になります。");
            }
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            refreshPurchases(true);
        } else if (responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            listener.onBillingMessage(toUserMessage(billingResult));
        }
    }

    private void launchPurchase(Activity activity, ProductDetails product, String offerToken) {
        if (!isReady()) {
            listener.onBillingMessage("Google Playに接続しています。少し待ってからもう一度お試しください。");
            start();
            return;
        }

        BillingFlowParams.ProductDetailsParams.Builder productBuilder =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product);
        if (offerToken != null && !offerToken.isEmpty()) {
            productBuilder.setOfferToken(offerToken);
        }
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productBuilder.build()))
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            listener.onBillingMessage(toUserMessage(result));
        }
    }

    private void handleUnavailableProduct(String planName) {
        if (!isReady()) {
            listener.onBillingMessage("Google Playに接続しています。少し待ってからもう一度お試しください。");
            start();
            return;
        }
        queryProductDetails();
        listener.onBillingMessage(planName + "を確認できません。Google Playからインストールしたアプリでお試しください。");
    }

    private void queryPurchases(String productType, PurchaseRefresh refresh) {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            boolean success = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
            if (success) {
                for (Purchase purchase : purchases) {
                    PremiumPlan plan = planForPurchase(purchase);
                    if (plan == PremiumPlan.NONE
                            || purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                        continue;
                    }
                    refresh.markActive(plan, purchase);
                    acknowledgeIfNeeded(purchase);
                }
            }
            refresh.completeQuery(success, billingResult);
        });
    }

    private void queryProductDetails() {
        if (!isReady()) {
            return;
        }

        monthlyProduct = null;
        lifetimeProduct = null;
        monthlyOfferToken = null;
        lifetimeOfferToken = null;
        productQueryFinished = false;
        ProductRefresh refresh = new ProductRefresh(++productQueryGeneration);
        queryProductDetailsForType(
                SUBSCRIPTION_PRODUCT_ID,
                BillingClient.ProductType.SUBS,
                refresh
        );
        queryProductDetailsForType(
                LIFETIME_PRODUCT_ID,
                BillingClient.ProductType.INAPP,
                refresh
        );
    }

    private void queryProductDetailsForType(
            String productId,
            String productType,
            ProductRefresh refresh
    ) {
        QueryProductDetailsParams.Product requestedProduct = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(requestedProduct))
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, queryResult) -> {
            if (refresh.generation != productQueryGeneration) {
                return;
            }
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                refresh.completeQuery();
                return;
            }

            for (ProductDetails productDetails : queryResult.getProductDetailsList()) {
                if (SUBSCRIPTION_PRODUCT_ID.equals(productDetails.getProductId())) {
                    configureMonthlyProduct(productDetails);
                } else if (LIFETIME_PRODUCT_ID.equals(productDetails.getProductId())) {
                    configureLifetimeProduct(productDetails);
                }
            }
            refresh.completeQuery();
        });
    }

    private void configureMonthlyProduct(ProductDetails product) {
        ProductDetails.SubscriptionOfferDetails selectedOffer = selectMonthlyOffer(product);
        if (selectedOffer == null) {
            return;
        }
        monthlyProduct = product;
        monthlyOfferToken = selectedOffer.getOfferToken();
        List<ProductDetails.PricingPhase> pricingPhases =
                selectedOffer.getPricingPhases().getPricingPhaseList();
        if (!pricingPhases.isEmpty()) {
            ProductDetails.PricingPhase recurringPhase =
                    pricingPhases.get(pricingPhases.size() - 1);
            monthlyPrice = recurringPhase.getFormattedPrice();
            if ("P1M".equals(recurringPhase.getBillingPeriod())) {
                monthlyPrice += " / 月";
            }
        }
    }

    private void configureLifetimeProduct(ProductDetails product) {
        List<ProductDetails.OneTimePurchaseOfferDetails> offers =
                product.getOneTimePurchaseOfferDetailsList();
        ProductDetails.OneTimePurchaseOfferDetails selectedOffer =
                offers == null || offers.isEmpty()
                        ? product.getOneTimePurchaseOfferDetails()
                        : offers.get(0);
        if (selectedOffer == null) {
            return;
        }
        lifetimeProduct = product;
        lifetimeOfferToken = selectedOffer.getOfferToken();
        lifetimePrice = selectedOffer.getFormattedPrice();
    }

    private ProductDetails.SubscriptionOfferDetails selectMonthlyOffer(ProductDetails product) {
        List<ProductDetails.SubscriptionOfferDetails> offers =
                product.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            return null;
        }
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            if (MONTHLY_BASE_PLAN_ID.equals(offer.getBasePlanId())) {
                return offer;
            }
        }
        return offers.get(0);
    }

    private PremiumPlan planForPurchase(Purchase purchase) {
        if (purchase.getProducts().contains(LIFETIME_PRODUCT_ID)) {
            return PremiumPlan.LIFETIME;
        }
        if (purchase.getProducts().contains(SUBSCRIPTION_PRODUCT_ID)) {
            return PremiumPlan.MONTHLY;
        }
        return PremiumPlan.NONE;
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (purchase.isAcknowledged() || !isReady()) {
            return;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                listener.onBillingMessage("購入の確認に失敗しました。通信状態を確認してください。");
            }
        });
    }

    private boolean isReady() {
        return billingClient != null && billingClient.isReady();
    }

    private String toUserMessage(BillingResult billingResult) {
        switch (billingResult.getResponseCode()) {
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                return "この端末ではGoogle Playのお支払いを利用できません。";
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "プレミアム商品は現在利用できません。";
            case BillingClient.BillingResponseCode.NETWORK_ERROR:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                return "通信できませんでした。インターネット接続を確認してください。";
            default:
                return "Google Playのお支払いを開始できませんでした。";
        }
    }

    private final class PurchaseRefresh {
        private final boolean userInitiated;
        private int completedQueries = 0;
        private int failedQueries = 0;
        private PremiumPlan activePlan = PremiumPlan.NONE;
        private boolean monthlyActive = false;
        private boolean monthlyAutoRenewing = false;
        private long monthlyPurchaseTime = 0L;
        private BillingResult lastFailure;

        private PurchaseRefresh(boolean userInitiated) {
            this.userInitiated = userInitiated;
        }

        private void markActive(PremiumPlan plan, Purchase purchase) {
            if (plan == PremiumPlan.LIFETIME || activePlan == PremiumPlan.NONE) {
                activePlan = plan;
            }
            if (plan == PremiumPlan.MONTHLY) {
                monthlyActive = true;
                monthlyAutoRenewing = purchase.isAutoRenewing();
                monthlyPurchaseTime = purchase.getPurchaseTime();
            }
        }

        private void completeQuery(boolean success, BillingResult billingResult) {
            completedQueries++;
            if (!success) {
                failedQueries++;
                lastFailure = billingResult;
            }
            if (completedQueries < 2) {
                return;
            }

            if (activePlan == PremiumPlan.NONE && failedQueries > 0) {
                if (userInitiated && lastFailure != null) {
                    listener.onBillingMessage(toUserMessage(lastFailure));
                }
                return;
            }

            boolean firstCheck = !firstPurchaseQueryCompleted;
            firstPurchaseQueryCompleted = true;
            listener.onPremiumStatusChanged(activePlan, firstCheck);
            listener.onMonthlySubscriptionStateChanged(
                    monthlyActive,
                    monthlyAutoRenewing,
                    monthlyPurchaseTime
            );
            if (userInitiated) {
                listener.onBillingMessage(activePlan == PremiumPlan.NONE
                        ? "有効なプレミアム購入は見つかりませんでした。"
                        : "購入情報を復元しました。");
            }
        }
    }

    private final class ProductRefresh {
        private final int generation;
        private int completedQueries = 0;

        private ProductRefresh(int generation) {
            this.generation = generation;
        }

        private void completeQuery() {
            completedQueries++;
            if (completedQueries < 2 || generation != productQueryGeneration) {
                return;
            }
            productQueryFinished = true;
            listener.onBillingProductChanged(isAnyProductAvailable());
        }
    }
}
