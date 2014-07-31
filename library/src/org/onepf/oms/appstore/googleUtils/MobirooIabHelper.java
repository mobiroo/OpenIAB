package org.onepf.oms.appstore.googleUtils;

import java.util.List;

import org.onepf.oms.Appstore;
import org.onepf.oms.OpenIabHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
public class MobirooIabHelper extends IabHelper {

	public final static String TAG = MobirooIabHelper.class.getSimpleName();
	
	public MobirooIabHelper(Context ctx, String base64PublicKey, Appstore appstore) {
		super(ctx, base64PublicKey, appstore);
		logDebug("MobirooIabHelper: Constructor");
	}

	@Override
	public void launchPurchaseFlow(Activity act, String sku, int requestCode, OnIabPurchaseFinishedListener listener) {
		logDebug("launchPurchaseFlow:");
		super.launchPurchaseFlow(act, sku, requestCode, listener);
	}

	@Override
	public void launchPurchaseFlow(Activity act, String sku, int requestCode, OnIabPurchaseFinishedListener listener, String extraData) {
		logDebug("launchPurchaseFlow:");
		super.launchPurchaseFlow(act, sku, requestCode, listener, extraData);
	}

	@Override
	public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, OnIabPurchaseFinishedListener listener, String extraData) {
		logDebug("launchPurchaseFlow:");
		super.launchPurchaseFlow(act, sku, itemType, requestCode, listener, extraData);
	}
	
	@Override
	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		
		logDebug("handleActivityResult(int requestCode, int resultCode, Intent data): (" + requestCode + ", " + resultCode + ", " + data + ")");
		IabResult result;
        if (requestCode != mRequestCode) return false;

        checkSetupDone("handleActivityResult");

        // end of async purchase operation
        flagEndAsync();

        if (data == null) {
            logError("Null data in IAB activity result.");
            result = new IabResult(IABHELPER_BAD_RESPONSE, "Null data in IAB result");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
            return true;
        }

        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        
        final int RESULT_USER_CANCELED = 1;
        // Begin Mobiroo: Handle return values correctly
        String appstoreName = appstore.getAppstoreName();
        if (appstoreName.equals(OpenIabHelper.NAME_MOBIROO)) {
            if (resultCode == BILLING_RESPONSE_RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {
                processPurchaseSuccess(data, purchaseData, dataSignature);
            } else if (resultCode == BILLING_RESPONSE_RESULT_OK) {
                // result code was OK, but in-app billing response was not OK.
                processPurchaseFail(responseCode);
            } else if (resultCode == RESULT_USER_CANCELED) {
                logDebug("Purchase canceled - Response: " + getResponseDesc(responseCode));
                result = new IabResult(IABHELPER_USER_CANCELLED, "User canceled.");
                if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
            } else {
                logError("Purchase failed. Result code: " + Integer.toString(resultCode)
                         + ". Response: " + getResponseDesc(responseCode));
                result = new IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response.");
                if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
            }
            return true;
        }
        
        return true;
        
	}

	@Override
	public void consume(Purchase itemInfo) throws IabException {
		logDebug("consume: ");
		super.consume(itemInfo);
	}

	@Override
	public void consumeAsync(Purchase purchase, OnConsumeFinishedListener listener) {
		logDebug("consumeAsync:");
		super.consumeAsync(purchase, listener);
	}

	@Override
	public void consumeAsync(List<Purchase> purchases, OnConsumeMultiFinishedListener listener) {
		logDebug("consumeAsync: ");
		super.consumeAsync(purchases, listener);
	}
	
	public void logDebug(String msg) {
        if (isDebugLog()) Log.d(TAG, msg);
    }

    void logError(String msg) {
        Log.e(TAG, "In-app billing error: " + msg);
    }

    void logWarn(String msg) {
        if (isDebugLog()) Log.w(TAG, "In-app billing warning: " + msg);
    }
	
	private static boolean isDebugLog() {
        return OpenIabHelper.isDebugLog();
    }
}
