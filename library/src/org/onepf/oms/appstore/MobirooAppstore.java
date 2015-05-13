package org.onepf.oms.appstore;

import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.IOpenAppstore;
import org.onepf.oms.IOpenInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.MobirooIabHelper;

import com.android.vending.billing.IInAppBillingService;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class MobirooAppstore extends OpenAppstore 
{
	public final static String TAG = MobirooAppstore.class.getSimpleName();
	public final static String MOBIROO_PACKAGE_NAME = "com.mobiroo.xgen"; 
	private static boolean isDebugLog() {
		return OpenIabHelper.isDebugLog();
	}
	
	public MobirooAppstore(Context context, String appstoreName, IOpenAppstore openAppstoreService, final Intent billingIntent,String publicKey, ServiceConnection serviceConn) {
		super(context, appstoreName, openAppstoreService, billingIntent, publicKey, serviceConn);
		if (isDebugLog()) Log.d(TAG, "MobirooAppstore: Constructor");
		
		billingIntent.setPackage(MOBIROO_PACKAGE_NAME);
		
		if (billingIntent != null) {
			//Set an explicit application package name of mobiroo store from "com.mobiroo.xgen
			billingIntent.setPackage(MOBIROO_PACKAGE_NAME);
			this.mBillingService = new MobirooIabHelper(context, publicKey, this)
			{
				@Override
                protected Intent getServiceIntent() {
                    return billingIntent;
                }
                @Override
                protected IInAppBillingService getServiceFromBinder(IBinder service) {
                    return new IOpenInAppBillingWrapper(IOpenInAppBillingService.Stub.asInterface(service));
                }
                @Override
                public void dispose() {
                    super.dispose();
                    MobirooAppstore.this.context.unbindService(MobirooAppstore.this.serviceConn);
                }
			};
		}
	}

	@Override
	public boolean isPackageInstaller(String packageName) {
		if (isDebugLog()) Log.d(TAG, "isPackageInstaller: " + packageName);
		return super.isPackageInstaller(packageName);
	}

	@Override
	public boolean isBillingAvailable(String packageName) {
		if (isDebugLog()) Log.d(TAG, "isPackageInstaller: " + packageName);
		return super.isBillingAvailable(packageName);
	}

	@Override
	public int getPackageVersion(String packageName) {
		if (isDebugLog()) Log.d(TAG, "isPackageInstaller: " + packageName);
		return super.getPackageVersion(packageName);
	}

	@Override
	public String getAppstoreName() {
		if (isDebugLog()) Log.d(TAG, "getAppstoreName() ");
		return super.getAppstoreName();
	}

	@Override
	public Intent getProductPageIntent(String packageName) {
		if (isDebugLog()) Log.d(TAG, "getProductPageIntent: " + packageName);
		return super.getProductPageIntent(packageName);
	}

	@Override
	public Intent getRateItPageIntent(String packageName) {
		if (isDebugLog()) Log.d(TAG, "getRateItPageIntent: " + packageName);
		return super.getRateItPageIntent(packageName);
	}

	@Override
	public Intent getSameDeveloperPageIntent(String packageName) {
		if (isDebugLog()) Log.d(TAG, "getSameDeveloperPageIntent: " + packageName);
		return super.getSameDeveloperPageIntent(packageName);
	}

	@Override
	public boolean areOutsideLinksAllowed() {
		if (isDebugLog()) Log.d(TAG, "areOutsideLinksAllowed()");
		return super.areOutsideLinksAllowed();
	}

	@Override
	public AppstoreInAppBillingService getInAppBillingService() {
		if (isDebugLog()) Log.d(TAG, "getInAppBillingService()");
		return super.getInAppBillingService();
	}

	@Override
	public String toString() {
		if (isDebugLog()) Log.d(TAG, "toString()");
		return super.toString();
	}
}
