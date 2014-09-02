package org.onepf.trivialdrive;

import org.json.JSONException;
import org.json.JSONObject;

public class VerifyPurchaseResponse
{
	final private String origData;
	final private String kind;
	final private long purchaseTime;
	final private int purchaseState;
	final private int consumptionState;
	final private String developerPayload;
	
	public VerifyPurchaseResponse(String jsonData) throws JSONException
	{
		this.origData = jsonData;
		JSONObject jsonObject = new JSONObject(jsonData);
		kind = jsonObject.getString("kind");
		purchaseTime = jsonObject.getLong("purchaseTime");
		purchaseState = jsonObject.getInt("purchaseState");
		consumptionState = jsonObject.getInt("consumptionState");
		developerPayload = jsonObject.getString("developerPayload");
	}

	public String getOrigData()
	{
		return origData;
	}

	public String getKind()
	{
		return kind;
	}

	public long getPurchaseTime()
	{
		return purchaseTime;
	}

	public int getPurchaseState()
	{
		return purchaseState;
	}

	public int getConsumptionState()
	{
		return consumptionState;
	}

	public String getDeveloperPayload()
	{
		return developerPayload;
	}

	@Override
	public String toString()
	{
		return "VerifyResponse [kind=" + kind + ", purchaseTime=" + purchaseTime + ", purchaseState=" + purchaseState + ", consumptionState="
				+ consumptionState + ", developerPayload=" + developerPayload + "]";
	}
}
