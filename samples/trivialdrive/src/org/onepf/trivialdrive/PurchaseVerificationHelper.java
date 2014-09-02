package org.onepf.trivialdrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;

import android.os.AsyncTask;
import android.util.Log;

public class PurchaseVerificationHelper
{
	public final static String TAG = PurchaseVerificationHelper.class.getSimpleName();
	
	// https://{channelname}.mobileplatform.solutions/api/v1.0/openiab/verify/{packagename}/inapp/{sku}/purchases/{token}
	public static VerifyPurchaseResponse verifyPurchase(String channel, String packagename, String sku, String token) throws ClientProtocolException, IOException, JSONException
	{
		Log.d(TAG, "verifyPurchase: channel= " + channel + ", packagename= " + packagename + ", sku= " + sku + ", token= " + token);
		HttpClient httpClient = new DefaultHttpClient();
		String requestPath = "http://%s.mobileplatform.solutions/api/v1.0/openiab/verify/%s/inapp/%s/purchases/%s";
		requestPath = String.format(requestPath, channel, packagename, sku, token);
		Log.d(TAG, "End Point URL= " + requestPath);
		HttpGet httpGet = new HttpGet(requestPath);
		
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 6 * 1000);
		HttpConnectionParams.setSoTimeout(httpParams, 6 * 1000);
		
		HttpResponse httpResponse;
		
		httpResponse = httpClient.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		
		Log.d(TAG, "Verify Purchase API Reponse Code= " + httpResponse.getStatusLine().getStatusCode());
		Log.d(TAG, "Verify Purchase API Reponse Line= " + httpResponse.getStatusLine().getReasonPhrase());
		
		if(httpResponse.getStatusLine().getStatusCode() == 200 && httpEntity!=null)
		{
			InputStream inputStream = httpEntity.getContent();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			while((line=bufferedReader.readLine())!=null)
			{
				stringBuilder.append(line);
				stringBuilder.append("\n");
			}
			
			String responseBody = stringBuilder.toString().trim();
			Log.v(TAG, "responseBody: " + responseBody);
			
			return new VerifyPurchaseResponse(responseBody);
		}
		else
		{
			throw new IOException("Failed to retrive data");
		}
	}
	
	public static void asyncVerifyPurchase(String channel, String packagename, String sku, String token, VerifyPurchaseListener verifyReceiptListener)
	{
		new VerifyReceiptTask(channel, packagename, sku, token, verifyReceiptListener).execute();
	}
	
	public static class VerifyReceiptTask extends AsyncTask<Void, Void, VerifyPurchaseResponse>
	{
		private String channel;
		private String packagename;
		private String sku;
		private String token;
		private VerifyPurchaseListener verifyPurchaseListener;
		
		public VerifyReceiptTask(String channel, String packagename, String sku, String token, VerifyPurchaseListener verifyPurchaseListener)
		{
			super();
			this.channel = channel;
			this.packagename = packagename;
			this.sku = sku;
			this.token = token;
			this.verifyPurchaseListener = verifyPurchaseListener;
		}

		@Override
		protected VerifyPurchaseResponse doInBackground(Void... params)
		{
			try
			{
				VerifyPurchaseResponse verifyResponse = verifyPurchase(channel, packagename, sku, token);
				return verifyResponse;
			}
			catch (ClientProtocolException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(VerifyPurchaseResponse result)
		{
			if(verifyPurchaseListener!=null)
			{
				if(result!=null)
					verifyPurchaseListener.onVerifySuccess(result);
				else 
					verifyPurchaseListener.onVerifyFailure();
			}
		}
		
	}
	
	public interface VerifyPurchaseListener
	{
		abstract public void onVerifySuccess(VerifyPurchaseResponse VerifyResponse);
		abstract public void onVerifyFailure();
	}
}
