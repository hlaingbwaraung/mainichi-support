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

    public interface Listener {
        void onPremiumStatusChanged(boolean active, boolean firstCheck);

        void onBillingProductChanged(boolean available);

        void onBillingMessage(String message);
    }

    private final Context appContext;
    private final Listener listener;
    private BillingClient billingClient;
    private ProductDetails premiumProduct;
    private String offerToken;
    private String formattedPrice = "¥300 / 月";
    private boolean firstPurchaseQueryCompleted = false;
    private boolean productQueryFinished = false;

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

    public boolean isProductAvailable() {
        return premiumProduct != null && offerToken != null;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public boolean isProductQueryFinished() {
        return productQueryFinished;
    }

    public void launchPurchase(Activity activity) {
        if (!isReady()) {
            listener.onBillingMessage("Google Playに接続しています。少し待ってからもう一度お試しください。");
            start();
            return;
        }
        if (!isProductAvailable()) {
            queryProductDetails();
            listener.onBillingMessage("定期購入の商品を確認できません。Google Playからインストールしたアプリでお試しください。");
            return;
        }

        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(premiumProduct)
                        .setOfferToken(offerToken)
                        .build();
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams))
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            listener.onBillingMessage(toUserMessage(result));
        }
    }

    public void refreshPurchases(boolean userInitiated) {
        if (!isReady()) {
            if (userInitiated) {
                listener.onBillingMessage("Google Playに接続しています。少し待ってからもう一度お試しください。");
            }
            start();
            return;
        }

        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                if (userInitiated) {
                    listener.onBillingMessage(toUserMessage(billingResult));
                }
                return;
            }

            boolean active = false;
            for (Purchase purchase : purchases) {
                if (isPremiumPurchase(purchase)
                        && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    active = true;
                    acknowledgeIfNeeded(purchase);
                }
            }

            boolean firstCheck = !firstPurchaseQueryCompleted;
            firstPurchaseQueryCompleted = true;
            listener.onPremiumStatusChanged(active, firstCheck);
            if (userInitiated) {
                listener.onBillingMessage(active
                        ? "購入情報を復元しました。"
                        : "有効なプレミアム会員登録は見つかりませんでした。");
            }
        });
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
            boolean active = false;
            boolean pending = false;
            for (Purchase purchase : purchases) {
                if (!isPremiumPurchase(purchase)) {
                    continue;
                }
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    active = true;
                    acknowledgeIfNeeded(purchase);
                } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                    pending = true;
                }
            }
            if (active) {
                listener.onPremiumStatusChanged(true, false);
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

    private void queryProductDetails() {
        if (!isReady()) {
            return;
        }

        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, queryResult) -> {
            premiumProduct = null;
            offerToken = null;
            productQueryFinished = true;
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                listener.onBillingProductChanged(false);
                return;
            }

            List<ProductDetails> products = queryResult.getProductDetailsList();
            if (products.isEmpty()) {
                listener.onBillingProductChanged(false);
                return;
            }

            premiumProduct = products.get(0);
            ProductDetails.SubscriptionOfferDetails selectedOffer = selectMonthlyOffer(premiumProduct);
            if (selectedOffer == null) {
                premiumProduct = null;
                listener.onBillingProductChanged(false);
                return;
            }

            offerToken = selectedOffer.getOfferToken();
            List<ProductDetails.PricingPhase> pricingPhases =
                    selectedOffer.getPricingPhases().getPricingPhaseList();
            if (!pricingPhases.isEmpty()) {
                ProductDetails.PricingPhase recurringPhase =
                        pricingPhases.get(pricingPhases.size() - 1);
                formattedPrice = recurringPhase.getFormattedPrice();
                if ("P1M".equals(recurringPhase.getBillingPeriod())) {
                    formattedPrice += " / 月";
                }
            }
            listener.onBillingProductChanged(true);
        });
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

    private boolean isPremiumPurchase(Purchase purchase) {
        return purchase.getProducts().contains(SUBSCRIPTION_PRODUCT_ID);
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
}
