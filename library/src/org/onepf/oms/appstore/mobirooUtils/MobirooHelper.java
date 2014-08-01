package org.onepf.oms.appstore.mobirooUtils;

import java.lang.reflect.Field;

import dalvik.system.PathClassLoader;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class MobirooHelper {

	@SuppressLint("NewApi")
	public static String getBaseUrl(Context context) {
		final String TAG = "getBaseUrl";
		try {
			if (android.os.Build.VERSION.SDK_INT >= 14) {
				Context appContext = context.createPackageContext(
						"com.mobiroo.xgen", Context.MODE_PRIVATE);
				Log.v(TAG, "appContext:" + appContext.getPackageName());
				PathClassLoader pathClassLoader = new PathClassLoader(
						appContext.getPackageCodePath(),
						appContext.getClassLoader());
				Class<?> c = pathClassLoader
						.loadClass("com.mobiroo.xgen.core.settings.MyBuildConfig");
				Field field = c.getDeclaredField("BASE_URL");
				String baseUrl = (String) field.get(null);
				Log.v(TAG, "baseUrl:" + baseUrl);
				return baseUrl;
			} else {
				return null;
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			return null;
		}
	}

	public static ApiClient buildInAppPurchaseConsumeClient(
			final String apiHost,
			final InAppPurchaseConsumeRequest inAppPurchaseConsumeRequest) {
		ApiClient apiClient = new ApiClientBuilder().setBaseUri(apiHost)
				.addPath("api/v1.0/openiab/consume")
				.addPath(inAppPurchaseConsumeRequest.getAndroid_id())
				.addPath(inAppPurchaseConsumeRequest.getPackage_name())
				.addPath(inAppPurchaseConsumeRequest.getOrder_uuid())
				.addHeader("Content-Type", MediaType.APPLICATION_JSON)
				.setHttpMethod(HttpMethod.GET).build();

		return apiClient;
	}

	public static int consumePurchase(final Context context,
			final String baseUrl,
			final InAppPurchaseConsumeRequest inAppPurchaseConsumeRequest) {
		final int RESULT_OK = 0;
		final int RESULT_ERROR = 6;
		final int RESULT_ITEM_NOT_OWNED = 8;

		try {
			HttpResponseResult httpResponseResult = buildInAppPurchaseConsumeClient(
					baseUrl, inAppPurchaseConsumeRequest).execute();
			if (httpResponseResult.getResponseCode() == 200
					|| httpResponseResult.getResponseCode() == 409) {
				return RESULT_OK;
			} else if (httpResponseResult.getResponseCode() == 404) {
				return RESULT_ITEM_NOT_OWNED;
			} else {
				return RESULT_ERROR;
			}
		} catch (Exception e) {
			return RESULT_ERROR;
		}

	}
}
