package org.onepf.oms.appstore.googleUtils;

import java.util.List;
import org.json.JSONObject;
import org.onepf.oms.Appstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.mobirooUtils.HttpResponseResult;
import org.onepf.oms.appstore.mobirooUtils.InAppPurchaseConsumeRequest;
import org.onepf.oms.appstore.mobirooUtils.MobirooHelper;
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

		try {
			JSONObject jsonObject = new JSONObject(purchaseData);
			String baseUrl = MobirooHelper.getBaseUrl(mContext);

			if (!jsonObject.has("developerPayload")
					|| jsonObject.getString("developerPayload") == null
					|| jsonObject.getString("developerPayload").trim().length() == 0) {
				android.net.Uri uri = android.net.Uri.parse(baseUrl);
				String host = uri.getHost();
				String[] split = host.split("\\.");
				jsonObject.put("developerPayload", split[0]);
				purchaseData = jsonObject.toString();
			}
		} catch (Exception e) {
			logError("handleActivityResult: " + e);
		}
        
        
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
		logDebug("consume: " + itemInfo);

		boolean consumePurchaseEnabled = MobirooHelper
				.isConsumePurchaseEnabled(mContext);
		logDebug("consume: consumePurchaseEnabled: " + consumePurchaseEnabled);
		if (consumePurchaseEnabled == true) {
			super.consume(itemInfo);
		} else {
			if (!itemInfo.mItemType.equals(ITEM_TYPE_INAPP)) {
				throw new IabException(IABHELPER_INVALID_CONSUMPTION,
						"Items of type '" + itemInfo.mItemType
								+ "' can't be consumed.");
			}

			final int RESULT_OK = 0;
			final int RESULT_ERROR = 6;
			final int RESULT_ITEM_NOT_OWNED = 8;
			MobirooHelper.setStrictModePolicy();
			String baseUrl = MobirooHelper.getBaseUrl(mContext);
			String android_id = MobirooHelper.getAndroidId(mContext);
			String package_name = itemInfo.getPackageName();
			String order_uuid = itemInfo.getToken();

			if (order_uuid == null || order_uuid.equals("")) {
				logError("Can't consume " + itemInfo.getSku() + ". No token.");
				throw new IabException(IABHELPER_MISSING_TOKEN,
						"PurchaseInfo is missing token for sku: "
								+ itemInfo.getSku() + " " + itemInfo);
			}

			logDebug("Consuming sku: " + itemInfo.getSku() + ", token: "
					+ itemInfo.getToken());

			InAppPurchaseConsumeRequest inAppPurchaseConsumeRequest = new InAppPurchaseConsumeRequest(
					order_uuid, android_id, package_name);

			try {
				HttpResponseResult httpResponseResult = MobirooHelper
						.consumePurchase(mContext, baseUrl,
								inAppPurchaseConsumeRequest);
				if (httpResponseResult.getResponseCode() == 200
						|| httpResponseResult.getResponseCode() == 409) {
					logDebug("Successfully consumed sku: " + itemInfo.getSku()
							+ ", Billing Response: " + RESULT_OK);
				} else if (httpResponseResult.getResponseCode() == 412) {
					logDebug("Error consuming consuming sku "
							+ itemInfo.getSku() + ". "
							+ getResponseDesc(RESULT_ITEM_NOT_OWNED));
					throw new IabException(RESULT_ITEM_NOT_OWNED,
							"Error consuming sku " + itemInfo.getSku());
				} else {
					logDebug("Error consuming consuming sku "
							+ itemInfo.getSku() + ". "
							+ getResponseDesc(RESULT_ERROR));
					throw new IabException(RESULT_ERROR, "Error consuming sku "
							+ itemInfo.getSku());
				}
			} catch (Exception e) {
				throw new IabException(IABHELPER_REMOTE_EXCEPTION,
						"Remote exception while consuming. PurchaseInfo: "
								+ itemInfo, e);
			}
		}
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

	@Override
	public boolean subscriptionsSupported() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
}
