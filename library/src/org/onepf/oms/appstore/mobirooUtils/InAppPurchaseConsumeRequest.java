package org.onepf.oms.appstore.mobirooUtils;


public class InAppPurchaseConsumeRequest
{
	final private String order_uuid;
	final private String android_id;
	final private String package_name;
	
	public InAppPurchaseConsumeRequest( String order_uuid, String android_id, String package_name)
	{
		super();
		this.order_uuid = order_uuid;
		this.android_id = android_id;
		this.package_name = package_name;
	}

	public String getOrder_uuid()
	{
		return order_uuid;
	}

	public String getAndroid_id()
	{
		return android_id;
	}

	public String getPackage_name()
	{
		return package_name;
	}
}
