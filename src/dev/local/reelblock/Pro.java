package dev.local.reelblock;

import android.app.Activity;
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

// Reel Block Pro: one Google Play in-app product, PRODUCT, bought once. The app talks only to the Play Store app
// on this phone (Play Billing Library, no INTERNET permission). Play is the source of truth: each time the app
// opens it asks Play what is owned and caches the answer in State.pro, which the service reads.
// ponytail: the cache is a private preference, so a rooted phone can flip it. Add server-side purchase
// verification if Pro ever unlocks something worth more than a one-time price.
final class Pro implements PurchasesUpdatedListener {
    static final String PRODUCT = "pro"; // Play Console > Monetize > In-app products: the same product ID
    private final Activity a;
    private final Runnable changed;
    private final BillingClient client;
    private ProductDetails details;
    boolean buying; // Play's purchase screen is open: MainActivity.onStop must not close the app

    Pro(Activity a, Runnable changed) {
        this.a = a;
        this.changed = changed;
        client = BillingClient.newBuilder(a).setListener(this).enableAutoServiceReconnection()
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build();
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult r) {
                if (r.getResponseCode() != BillingClient.BillingResponseCode.OK) return;
                refresh();
                client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT)
                        .setProductType(BillingClient.ProductType.INAPP).build())).build(), (res, result) -> {
                    List<ProductDetails> l = result.getProductDetailsList();
                    if (res.getResponseCode() == BillingClient.BillingResponseCode.OK && !l.isEmpty()) {
                        details = l.get(0);
                        done();
                    }
                });
            }

            @Override
            public void onBillingServiceDisconnected() {} // enableAutoServiceReconnection reconnects on the next call
        });
    }

    // Restores Pro after a reinstall or on a new phone, and removes it after a refund.
    private void refresh() {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            (r, list) -> { if (r.getResponseCode() == BillingClient.BillingResponseCode.OK) apply(list, true); });
    }

    @Override
    public void onPurchasesUpdated(BillingResult r, List<Purchase> list) {
        buying = false;
        if (r.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) apply(list, false);
        else if (r.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) refresh();
        else done();
    }

    // complete: this is the full list Play has, so "not in it" means "not owned".
    private void apply(List<Purchase> list, boolean complete) {
        boolean owned = false;
        for (Purchase p : list) {
            // PENDING (for example cash at a shop) is not paid yet: Pro comes on a later refresh.
            if (!p.getProducts().contains(PRODUCT) || p.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            owned = true;
            // Play refunds a purchase that is not acknowledged within 3 days.
            if (!p.isAcknowledged()) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(p.getPurchaseToken()).build(), x -> {});
        }
        if (owned || complete) State.setPro(a, owned);
        done();
    }

    private void done() {
        a.runOnUiThread(() -> { if (!a.isDestroyed()) changed.run(); });
    }

    // The local price, for example "€4.99", or null before Play answers (or with no Play Store).
    String price() {
        ProductDetails.OneTimePurchaseOfferDetails o = details == null ? null : details.getOneTimePurchaseOfferDetails();
        return o == null ? null : o.getFormattedPrice();
    }

    // false: Play cannot sell it now (no Play Store, not signed in, or no network). The caller explains.
    boolean buy() {
        if (details == null) return false;
        buying = true;
        BillingResult r = client.launchBillingFlow(a, BillingFlowParams.newBuilder().setProductDetailsParamsList(
            Collections.singletonList(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())).build());
        buying = r.getResponseCode() == BillingClient.BillingResponseCode.OK;
        return buying;
    }

    void end() {
        client.endConnection();
    }
}
