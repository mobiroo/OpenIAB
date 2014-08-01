package org.onepf.oms.appstore.mobirooUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.onepf.oms.OpenIabHelper;

import dalvik.system.PathClassLoader;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

public class MobirooHelper {

	public final static String TAG = MobirooHelper.class.getSimpleName();

	@SuppressLint("NewApi")
	public static final void setStrictModePolicy() {
		if (android.os.Build.VERSION.SDK_INT >= 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
	}

	public static String getBaseUrl(Context context) {
		try {
			Context appContext = context.createPackageContext(
					"com.mobiroo.xgen", Context.MODE_PRIVATE);
			logDebug("getBaseUrl: appContext:" + appContext.getPackageName());
			PathClassLoader pathClassLoader = new PathClassLoader(
					appContext.getPackageCodePath(),
					appContext.getClassLoader());
			Class<?> c = Class.forName(
					"com.mobiroo.xgen.core.settings.MyBuildConfig", false,
					pathClassLoader);
			Field field = c.getDeclaredField("BASE_URL");
			String baseUrl = (String) field.get(null);
			logDebug("getBaseUrl: baseUrl:" + baseUrl);
			return baseUrl;
		} catch (Exception e) {
			logError("getBaseUrl: Error: " + e);
			return null;
		}
	}

	public static boolean isConsumePurchaseEnabled(Context context) {
		try {

			Context appContext = context.createPackageContext(
					"com.mobiroo.xgen", Context.MODE_PRIVATE);
			logDebug("appContext:" + appContext.getPackageName());
			PathClassLoader pathClassLoader = new PathClassLoader(
					appContext.getPackageCodePath(),
					appContext.getClassLoader());
			Class<?> c = Class.forName(
					"com.mobiroo.xgen.core.openiab.settings.IAPSettings",
					false, pathClassLoader);
			Field field = c.getDeclaredField("CONSUME_PURCHASE_ENABLED");
			boolean enabled = field.getBoolean(null);
			logDebug( "isConsumePurchaseEnabled: CONSUME_PURCHASE_ENABLED:" + enabled);
			return enabled;
		} catch (Exception e) {
			logError("isConsumePurchaseEnabled: Error:" + e);
			return false;
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

		logDebug("buildInAppPurchaseConsumeClient: getCombinedPath: " + apiClient.getHttpRequest().getCombinedPath());
		
		return apiClient;
	}

	public static String getAndroidId(Context context) {

		try {
			Context appContext = context.createPackageContext(
					"com.mobiroo.xgen", Context.MODE_PRIVATE);
			logDebug("getAndroidId: appContext:" + appContext.getPackageName());
			PathClassLoader pathClassLoader = new PathClassLoader(
					appContext.getPackageCodePath(),
					appContext.getClassLoader());
			Class<?> c = Class.forName(
					"com.mobiroo.xgen.core.profiler.HardwareRenderer", false,
					pathClassLoader);
			Method method = c.getMethod("getAndroidID", Context.class);
			String androidId = (String) method.invoke(null, appContext);
			return androidId;
		} catch (Exception e) {
			logError("getAndroidId Error: " + e);
			return null;
		}
	}

	public static HttpResponseResult consumePurchase(final Context context,
			final String baseUrl,
			final InAppPurchaseConsumeRequest inAppPurchaseConsumeRequest) throws Exception {
		logDebug("consumePurchase: ");
		HttpResponseResult httpResponseResult = buildInAppPurchaseConsumeClient(baseUrl, inAppPurchaseConsumeRequest).execute();
		return httpResponseResult;
	}
	
	public static void logDebug(String msg) {
        if (isDebugLog()) Log.d(TAG, msg);
    }

    static void logError(String msg) {
        Log.e(TAG, "In-app billing error: " + msg);
    }

    static void logWarn(String msg) {
        if (isDebugLog()) Log.w(TAG, "In-app billing warning: " + msg);
    }
	
	private static boolean isDebugLog() {
        return OpenIabHelper.isDebugLog();
    }
}
