/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.trivialdrive;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.OpenIabHelper.Options;
import org.onepf.oms.appstore.AmazonAppstore;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.Security;
import org.onepf.trivialdrive.PurchaseVerificationHelper.VerifyPurchaseListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


/**
 * Example game using in-app billing version 3.
 * 
 * Before attempting to run this sample, please read the README file. It
 * contains important information on how to set up this project.
 * 
 * All the game-specific logic is implemented here in MainActivity, while the 
 * general-purpose boilerplate that can be reused in any app is provided in the 
 * classes in the util/ subdirectory. When implementing your own application,
 * you can copy over util/*.java to make use of those utility classes.  
 * 
 * This game is a simple "driving" game where the player can buy gas
 * and drive. The car has a tank which stores gas. When the player purchases
 * gas, the tank fills up (1/4 tank at a time). When the player drives, the gas
 * in the tank diminishes (also 1/4 tank at a time).
 *
 * The user can also purchase a "premium upgrade" that gives them a red car
 * instead of the standard blue one (exciting!).
 * 
 * The user can also purchase a subscription ("infinite gas") that allows them
 * to drive without using up any gas while that subscription is active.
 *
 * It's important to note the consumption mechanics for each item.
 *
 * PREMIUM: the item is purchased and NEVER consumed. So, after the original
 * purchase, the player will always own that item. The application knows to
 * display the red car instead of the blue one because it queries whether
 * the premium "item" is owned or not.
 * 
 * INFINITE GAS: this is a subscription, and subscriptions can't be consumed.
 *
 * GAS: when gas is purchased, the "gas" item is then owned. We consume it
 * when we apply that item's effects to our app's world, which to us means
 * filling up 1/4 of the tank. This happens immediately after purchase!
 * It's at this point (and not when the user drives) that the "gas"
 * item is CONSUMED. Consumption should always happen when your game
 * world was safely updated to apply the effect of the purchase. So,
 * in an example scenario:
 *
 * BEFORE:      tank at 1/2
 * ON PURCHASE: tank at 1/2, "gas" item is owned
 * IMMEDIATELY: "gas" is consumed, tank goes to 3/4
 * AFTER:       tank at 3/4, "gas" item NOT owned any more
 *
 * Another important point to notice is that it may so happen that
 * the application crashed (or anything else happened) after the user
 * purchased the "gas" item, but before it was consumed. That's why,
 * on startup, we check if we own the "gas" item, and, if so,
 * we have to apply its effects to our world and consume it. This
 * is also very important!
 *
 * @author Bruno Oliveira (Google)
 */
public class MainActivity extends Activity {
    // Debug tag, for logging
    static final String TAG = "TrivialDrive";

    // Does the user have the premium upgrade?
    boolean mIsPremium = false;
    
    // Does the user have an active subscription to the infinite gas plan?
    boolean mSubscribedToInfiniteGas = false;

    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    static final String SKU_PREMIUM = "sku_premium";
    static final String SKU_GAS = "sku_gas";
    
    // SKU for our subscription (infinite gas)
    static final String SKU_INFINITE_GAS = "sku_infinite_gas";
    
    static final String DEVELOPER_PAYLOAD = "Trivial_Drive_Payload_Random_123";
    
	final public String MOBIROO_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkd5P6tcVYNa4xFk"
			+ "KHt592HlOYrFq9T1OlDvkFpkdN9o+0cvTF2iQ8fjPQJ7eG6cr7Hc7Ch0mRtr1g4LFLQI92OrLAGTCag5Uyk/z4u"
			+ "KZUTtN/o54PSWMx16XiuzaK66MWmNfTnEVTb4uYXqnajZkNjfA6QdxT03c3zMLUwuf0kKwSaRjmWyY7Rb+mjRmx"
			+ "f09FIktB9fZPg1LSXXISalW4SC5hH7CYPxca1NuJmLq1uN0ANRgchh6jVXOhb+IAp9BugjlJ0LAFzQU76mOHoon"
			+ "yy/kD1abGo/WruKh8+LuCrjvV0p7EJxR0AZEFnGystjVLwLsMPhHs7q7Tp77NplhVwIDAQAB";
    
    static {
        OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.premium");
        OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_TSTORE, "tstore_sku_premium");
        OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003746");
        OpenIabHelper.mapSku(SKU_PREMIUM, "com.yandex.store", "org.onepf.trivialdrive.premium");
        OpenIabHelper.mapSku(SKU_PREMIUM, "Appland", "org.onepf.trivialdrive.premium");
		OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_NOKIA, "1023608");
		OpenIabHelper.mapSku(SKU_PREMIUM, "SlideME", "slideme_sku_premium");
		OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_MOBIROO, "mobiroo_sku_premium");

        OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.gas");
        OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_TSTORE, "tstore_sku_gas");
        OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003744");
        OpenIabHelper.mapSku(SKU_GAS, "com.yandex.store", "org.onepf.trivialdrive.gas");
        OpenIabHelper.mapSku(SKU_GAS, "Appland", "org.onepf.trivialdrive.gas");
		OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_NOKIA, "1023609");
		OpenIabHelper.mapSku(SKU_GAS, "SlideME", "slideme_sku_gas");
        OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_MOBIROO, "mobiroo_sku_gas");

        OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.infinite_gas");
        OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_TSTORE, "tstore_sku_infinite_gas");
        OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003747");
        OpenIabHelper.mapSku(SKU_INFINITE_GAS, "com.yandex.store", "org.onepf.trivialdrive.infinite_gas");
		OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_NOKIA, "1023610");
		OpenIabHelper.mapSku(SKU_INFINITE_GAS, "SlideME", "slideme_sku_inifinite_gas");
    }
    
    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    // Graphics for the gas gauge
    static int[] TANK_RES_IDS = { R.drawable.gas0, R.drawable.gas1, R.drawable.gas2,
                                   R.drawable.gas3, R.drawable.gas4 };

    // How many units (1/4 tank is our unit) fill in the tank.
    static final int TANK_MAX = 4;

    // Current amount of gas in tank, in units
    int mTank;

    // The helper object
    OpenIabHelper mHelper;

    /** is bililng setup is completed */
    private boolean setupDone = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // load game data
        loadData();

        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        String base64EncodedPublicKey   = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8A4rv1uXF5mqJGrtGkQ5PQGpyNIgcZhvRD3yNLC5T+NlIlvMlkuGUmgZnXHfPdORZT/s5QXa2ytjffOyDVgXpHrZ0J9bRoR+hePP4o0ANzdEY/ehkt0EsifB2Kjhok+kTNpikplwuFtIJnIyFyukcesPAXksu2LTQAEzYwlMeJ8W4ToDHw6U5gEXLZcMKiDVTFA0pb89wVfb76Uerv9c6lrydKZiTn/gxg8J1yrz7vNzX7IzoWPO0+pXLnkcgqtEHePF2DIW1D29GkNJOt6xH3IvyS4ZI+1xs3wuSg8vWq3fQP/XIVHZQOqd5pmJY0tdgzboHuqq3ebtNrBI6Ky0SwIDAQAB";
        String YANDEX_PUBLIC_KEY        = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs4NKNVt1lC97e5qr5qIK31WKh470ihgFdRSiV/8kdKtdk2gsLD70AFPFZ0py/OOyZflDjTOya809mU0lsWOxrrGZBRFqQKbvCPh9ZIMVZc79Uz0UZfjBy/n2h4bc0Z5VeBIsnDNh4DCD/XlHYwLIf6En+uPkKZwD3lG2JW4q4Hmuc3HYbuagv+hMexEG/umjbHTRq5rJ+rJ2LyYQs5Kdi/UZ5JKjsk9CuYrzMi9TqOqc9fDG19mfqqr4lfzvKneGIG11c3d1yUNX/MmSE43QYPPWNNKgGLha1AbS7RvtbWzEviiEZ0wjQkRSu4QAXhUurzK75eWDBN2KiJK9mlI1lQIDAQAB";
        String APPLAND_PUBLIC_KEY       = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC5idC9c24V7a7qCJu7kdIyOZskW0Rc7/q+K+ujEXsUaAdb5nwmlOJqpoJeCh5Fmq5A1NdF3BwkI8+GwTkH757NBZASSdEuN0pLZmA6LopOiMIy0LoIWknM5eWMa3e41CxCEFoMv48gFIVxDNJ/KAQAX7+KysYzIdlA3W3fBXXyGQIDAQAB";
        String SLIDEME_PUBLIC_KEY		= "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq6rFm2wb9smbcowrfZHYw71ISHYxF/tG9Jn9c+nRzFCVDSXjvedBxKllw16/GEx9DQ32Ut8azVAznB2wBDNUsSM8nzNhHeCSDvEX2/Ozq1dEq3V3DF4jBEKDAkIOMzIBRWN8fpA5MU/9m8QD9xkJDfP7Mw/6zEMidk2CEE8EZRTlpQ8ULVgBlFISd8Mt9w8ZFyeTyJTZhF2Z9+RZN8woU+cSXiVRmiA0+v2R8Pf+YNJb9fdV5yvM8r9K1MEdRaXisJyMOnjL7H2mZWigWLm7uGoUGuIg9HHi09COBMm3dzAe9yLZoPSG75SvYDsAZ6ms8IYxF6FAniNqfMOuMFV8zwIDAQAB";

        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please put your app's public key in MainActivity.java. See README.");
        }
        if (getPackageName().startsWith("com.example")) {
            throw new RuntimeException("Please change the sample's package name! See README.");
        }
        
        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        Map<String, String> storeKeys = new HashMap<String, String>();
        storeKeys.put(OpenIabHelper.NAME_GOOGLE, base64EncodedPublicKey);
//      storeKeys.put(OpenIabHelper.NAME_AMAZON, "Unavailable. Amazon doesn't support RSA verification. So this mapping is not needed"); //
//      storeKeys.put(OpenIabHelper.NAME_SAMSUNG,"Unavailable. SamsungApps doesn't support RSA verification. So this mapping is not needed"); //
        storeKeys.put("com.yandex.store", YANDEX_PUBLIC_KEY);
        storeKeys.put("Appland", APPLAND_PUBLIC_KEY);
        storeKeys.put("SlideME", SLIDEME_PUBLIC_KEY);
        
        
        Log.d(TAG, "=============== Registering Mobiroo PUBLIC KEY =================");
        storeKeys.put(OpenIabHelper.NAME_MOBIROO, MOBIROO_PUB_KEY);

        Options options = new Options();
		options.storeKeys = storeKeys;
		options.verifyMode = Options.VERIFY_ONLY_KNOWN;
		
        mHelper = new OpenIabHelper(this, options);
        
        // enable debug logging (for a production application, you should set this to false).
        //mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                setupDone = true;
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            
            verifyInventoryPurchases(inventory);
            
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");
            
            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */
            
            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
            
            // Do we have the infinite gas plan?
            Purchase infiniteGasPurchase = inventory.getPurchase(SKU_INFINITE_GAS);
            mSubscribedToInfiniteGas = (infiniteGasPurchase != null && 
                    verifyDeveloperPayload(infiniteGasPurchase));
            Log.d(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE") 
                        + " infinite gas subscription.");
            if (mSubscribedToInfiniteGas) mTank = TANK_MAX;

            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
            Purchase gasPurchase = inventory.getPurchase(SKU_GAS);
            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
                Log.d(TAG, "We have gas. Consuming it.");
                mHelper.consumeAsync(inventory.getPurchase(SKU_GAS), mConsumeFinishedListener);
                return;
            }

            updateUi();
            setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    private void verifyInventoryPurchases(Inventory inventory)
    {
    	Log.d(TAG, "verifyInventoryPurchases");
    	if(inventory!=null)
    	{
	    	List<Purchase> purchases = inventory.getAllPurchases();
	        if(purchases!=null)
	        {
	        	Log.d(TAG, "================== START validating purchase history =======================");
	        	for(Purchase purchase:purchases)
	        	{
	        		Log.d(TAG, "-----------------------------------------------------------");
	        		String origJson = purchase.getOriginalJson();
	        		String signature = purchase.getSignature();
	        		Log.d(TAG, "Validating purchase ===> "  + origJson);
	        		Log.d(TAG, "Signature ===> " + signature);
	        		boolean valid = manualPurchaseSignatureVerify(origJson, signature);
	        		String str = (valid)?"VALID":"NOT VALID";
	        		Log.d(TAG, "RESULT: The signature is==> " + str);
	        		Log.d(TAG, "-----------------------------------------------------------");
	        	}
	        	Log.d(TAG, "================== END validating purchase history =======================");
	        }
    	}
    }
    
    // User clicked the "Buy Gas" button
    public void onBuyGasButtonClicked(View arg0) {
        Log.d(TAG, "Buy gas button clicked.");

        if (mSubscribedToInfiniteGas) {
            complain("No need! You're subscribed to infinite gas. Isn't that awesome?");
            return;
        }
        
        if (mTank >= TANK_MAX) {
            complain("Your tank is full. Drive around a bit!");
            return;
        }

        if (!setupDone) {
            complain("Billing Setup is not completed yet");
            return;
        }
        // launch the gas purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
        setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for gas.");
        
        /* TODO: for security, generate your payload here for verification. See the comments on 
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use 
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = DEVELOPER_PAYLOAD; 
        
        mHelper.launchPurchaseFlow(this, SKU_GAS, RC_REQUEST, 
                mPurchaseFinishedListener, payload);
    }

    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked(View arg0) {
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
        
        if (!setupDone) {
            complain("Billing Setup is not completed yet");
            return;
        }

        setWaitScreen(true);
        
        /* TODO: for security, generate your payload here for verification. See the comments on 
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use 
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = DEVELOPER_PAYLOAD; 

        mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST, 
                mPurchaseFinishedListener, payload);
    }
    
    // "Subscribe to infinite gas" button clicked. Explain to user, then start purchase
    // flow for subscription.
    public void onInfiniteGasButtonClicked(View arg0) {
        if (!setupDone) {
            complain("Billing Setup is not completed yet");
            return;
        }

        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            return;
        }
        
        /* TODO: for security, generate your payload here for verification. See the comments on 
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use 
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = DEVELOPER_PAYLOAD; 
        
        setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for infinite gas subscription.");
        mHelper.launchPurchaseFlow(this,
                SKU_INFINITE_GAS, IabHelper.ITEM_TYPE_SUBS, 
                RC_REQUEST, mPurchaseFinishedListener, payload);        
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        Log.d(TAG, "startActivityForResult() intent: " + intent + " requestCode: " + requestCode);
        super.startActivityForResult(intent, requestCode);
    }

    private String mChannelId = "";
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	Log.d(TAG, "============== Processing purchase on activity result =================");
        Log.d(TAG, "onActivityResult() requestCode: " + requestCode+ " resultCode: " + resultCode+ " data: " + data);

        if(data!=null)
        {
	        String purhcaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
			String purchaseSignedData = data.getStringExtra("INAPP_DATA_SIGNATURE");
			String channelId = data.getStringExtra("CHANNEL_ID");
			
			this.mChannelId = channelId;
			
			Log.d(TAG, ": onActivityResult: purhcaseData: " + purhcaseData);
			Log.d(TAG, ": onActivityResult: purchaseSignedData: " + purchaseSignedData);
			Log.d(TAG, ": onActivityResult: channelId: " + channelId);
	        
			Log.d(TAG, "===== DO Manual sign verification on onActivityResult ============");
			boolean valid = Security.verifyPurchase(MOBIROO_PUB_KEY, purhcaseData, purchaseSignedData);
			Log.d(TAG, "manualPurchaseSignatureVerify: Result= " + ((valid)?"Purchase signed data is VALID":"Purchase signed data is NOT VALID"));
        }
        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else 
        {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
    
    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
        Log.d(TAG, "================ Verifying the developer payload =======================");
        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         * 
         * WARNING: Locally generating a random string when starting a purchase and 
         * verifying it here might seem like a good approach, but this will fail in the 
         * case where the user purchases an item on one device and then uses your app on 
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         * 
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         * 
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on 
         *    one device work on other devices owned by the user).
         * 
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        
        boolean result = DEVELOPER_PAYLOAD.equals(payload);
        
        Log.i(TAG, "verifyDeveloperPayload: Sent Payload: " + DEVELOPER_PAYLOAD   + ", Recieved Payload: " + payload);
        if(result)
        {
        	Log.d(TAG, "verifyDeveloperPayload: for Purchase: SKU: " + p.getSku() + ", SUCCESS");
        }
        else
        {
        	Log.e(TAG, "verifyDeveloperPayload: for Purchase: SKU: " + p.getSku() + ", Failed");
        }
        
        return result;
        
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) 
            {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) 
            {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            if(!callVerifyPurchase(purchase))
            {
            	complain("Error purchasing. Api Service for receipt verification has just failed");
                setWaitScreen(false);
                return;
            }
            
            if(manualPurchaseSignatureVerify(purchase.getOriginalJson(), purchase.getSignature()))
            {
            	Log.i(TAG, "Purchase signature verification SUCCESS.");
            }
            else
            {
            	Log.e(TAG, "Purchase signature verification FAILD.");
            }
            
            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_GAS)) 
            {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is gas. Starting gas consumption.");
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            }
            else if (purchase.getSku().equals(SKU_PREMIUM)) 
            {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                alert("Thank you for upgrading to premium!");
                mIsPremium = true;
                updateUi();
                setWaitScreen(false);
            }
            else if (purchase.getSku().equals(SKU_INFINITE_GAS)) 
            {
                // bought the infinite gas subscription
                Log.d(TAG, "Infinite gas subscription purchased.");
                alert("Thank you for subscribing to infinite gas!");
                mSubscribedToInfiniteGas = true;
                mTank = TANK_MAX;
                updateUi();
                setWaitScreen(false);
            }
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
                mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
                saveData();
                alert("You filled 1/4 tank. Your tank is now " + String.valueOf(mTank) + "/4 full!");
            }
            else {
                complain("Error while consuming: " + result);
            }
            updateUi();
            setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };

    // Drive button clicked. Burn gas!
    public void onDriveButtonClicked(View arg0) {
        Log.d(TAG, "Drive button clicked.");
        if (!mSubscribedToInfiniteGas && mTank <= 0) alert("Oh, no! You are out of gas! Try buying some!");
        else {
            if (!mSubscribedToInfiniteGas) --mTank;
            saveData();
            alert("Vroooom, you drove a few miles.");
            updateUi();
            Log.d(TAG, "Vrooom. Tank is now " + mTank);
        }
    }
    
    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    // updates UI to reflect model
    public void updateUi() {
        // update the car color to reflect premium status or lack thereof
        ((ImageView)findViewById(R.id.free_or_premium)).setImageResource(mIsPremium ? R.drawable.premium : R.drawable.free);

        // "Upgrade" button is only visible if the user is not premium
        findViewById(R.id.upgrade_button).setVisibility(mIsPremium ? View.GONE : View.VISIBLE);

        // "Get infinite gas" button is only visible if the user is not subscribed yet
        findViewById(R.id.infinite_gas_button).setVisibility(mSubscribedToInfiniteGas ? 
                View.GONE : View.VISIBLE);

        // update gas gauge to reflect tank status
        if (mSubscribedToInfiniteGas) {
            ((ImageView)findViewById(R.id.gas_gauge)).setImageResource(R.drawable.gas_inf);
        }
        else {
            int index = mTank >= TANK_RES_IDS.length ? TANK_RES_IDS.length - 1 : mTank;
            ((ImageView)findViewById(R.id.gas_gauge)).setImageResource(TANK_RES_IDS[index]);
        }        
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        if (AmazonAppstore.hasAmazonClasses()) { // Amazon moderators don't allow alert dialogs for in-apps
            Toast.makeText(this, "Welcome back, Driver!", Toast.LENGTH_SHORT).show();
        } else {
            alert("Error: " + message);
        }
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    void saveData() {
        
        /*
         * WARNING: on a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the data using a
         * SharedPreferences.
         */
        
        SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
        spe.putInt("tank", mTank);
        spe.commit();
        Log.d(TAG, "Saved data: tank = " + String.valueOf(mTank));
    }

    void loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mTank = sp.getInt("tank", 2);
        Log.d(TAG, "Loaded data: tank = " + String.valueOf(mTank));
    }

    
	public void onConsumeClicked(View view) {
		try {

			JSONObject jsonObject = new JSONObject();

			jsonObject.put("orderId", "TEST1234");
			jsonObject.put("packageName", getPackageName());
			jsonObject.put("productId", "TEST1234");
			jsonObject.put("purchaseTime", 123456);
			jsonObject.put("purchaseState", 1);
			jsonObject.put("developerPayload", "TEST1234");
			jsonObject.put("purchaseToken", "TEST1234");
			
			Purchase purchase = new Purchase("inapp", jsonObject.toString(), "", "com.mobiroo.xgen");
			
			mHelper.consumeAsync(purchase, mConsumeFinishedListener);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@SuppressLint("NewApi")
	public boolean callVerifyPurchase(Purchase purchase)
	{
		Log.w(TAG, " ========= Verify Purchase warning network operation on UI Thread =============");
		Log.d(TAG, "callVerifyPurchase: purchase: " + purchase.getOriginalJson());
		
		String packagename = getPackageName();
		String sku = getOriginalSkuFromPurchase(purchase);
		String token = purchase.getToken();
		
		Log.w(TAG, " Verify Purchase warning: Setting Thread Policy to permitAll");
		android.os.StrictMode.ThreadPolicy policy = new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build();
		android.os.StrictMode.setThreadPolicy(policy);
		
		try
		{
			VerifyPurchaseResponse verifyPurchaseResponse = PurchaseVerificationHelper.verifyPurchase(mChannelId, packagename, sku, token);
			Log.d(TAG, "Verify Purchase success: verifyPurchaseResponse= " + verifyPurchaseResponse.toString());
			Log.d(TAG, "callAsyncVerifyPurchase: onVerifySuccess: VerifyResponse: " + verifyPurchaseResponse);
			Log.d(TAG, "callAsyncVerifyPurchase: onVerifySuccess: Sent Developer Payload: " + DEVELOPER_PAYLOAD);
			Log.d(TAG, "callAsyncVerifyPurchase: onVerifySuccess: Received Developer Payload: " + verifyPurchaseResponse.getDeveloperPayload());
			boolean isValid = DEVELOPER_PAYLOAD.equals(verifyPurchaseResponse.getDeveloperPayload());
			if(isValid)
				Log.d(TAG, "callAsyncVerifyPurchase: Purchase Receipt is VALID");
			else 
				Log.e(TAG, "callAsyncVerifyPurchase: Purchase Receipt  is NOT VALID");
			
			return isValid;
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
			Log.e(TAG, "callVerifyPurchase: ClientProtocolException: Error:" + e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Log.e(TAG, "callVerifyPurchase: IOException: Error:" + e);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			Log.e(TAG, "callVerifyPurchase: JSONException: Error:" + e);
		}
		return false;
	}
	public void callAsyncVerifyPurchase(Purchase purchase)
	{
		Log.d(TAG, " ======= Calling verify purchase on Async Task =============");
		String packagename = getPackageName();
		String sku = getOriginalSkuFromPurchase(purchase);
		String token = purchase.getToken();
		
		PurchaseVerificationHelper.asyncVerifyPurchase(mChannelId, packagename, sku, token, new VerifyPurchaseListener()
		{
			
			@Override
			public void onApiVerifyServiceSuccess(VerifyPurchaseResponse verifyPurchaseResponse)
			{
				Log.d(TAG, "Verify Purchase success: onApiVerifyServiceSuccess= " + verifyPurchaseResponse.toString());
				Log.d(TAG, "callAsyncVerifyPurchase: onVerifySuccess: VerifyResponse: " + verifyPurchaseResponse);
				Log.d(TAG, "callAsyncVerifyPurchase: onVerifySuccess: Sent Developer Payload: " + DEVELOPER_PAYLOAD);
				Log.d(TAG, "callAsyncVerifyPurchase: onVerifySuccess: Received Developer Payload: " + verifyPurchaseResponse.getDeveloperPayload());
				boolean isValid = DEVELOPER_PAYLOAD.equals(verifyPurchaseResponse.getDeveloperPayload());
				if(isValid)
					Log.d(TAG, "callAsyncVerifyPurchase: Purchase Receipt is VALID");
				else 
					Log.e(TAG, "callAsyncVerifyPurchase: Purchase Receipt  is NOT VALID");
			}
			
			@Override
			public void onApiVerifyServiceFailure(Exception exception)
			{
				Log.e(TAG, "Verify Purchase Failed: onApiVerifyServiceFailure= Error: " + exception);
			}
		});
	}
	
	
	public boolean manualPurchaseSignatureVerify(String origJson, String signature)
	{
		Log.d(TAG, "================== Starting Manual Purchase Signature Verify =========================");
		boolean result = Security.verifyPurchase(MOBIROO_PUB_KEY, origJson, signature);
		Log.d(TAG, "manualPurchaseSignatureVerify: Result= " + ((result)?"Purchase signed data is VALID":"Purchase signed data is NOT VALID"));
		return result;
	}
	
	private String getOriginalSkuFromPurchase(Purchase purchase)
	{
		Log.d(TAG, "getOriginalSkuFromPurchase: purchase: " + purchase.getOriginalJson());
		try
		{
			String origJSON = purchase.getOriginalJson();
			JSONObject o = new JSONObject(origJSON);
	        String mSku = o.optString("productId");
	        Log.d(TAG, "getOriginalSkuFromPurchase: Original SKU: " + mSku);
	        return mSku;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Log.e(TAG, "getOriginalSkuFromPurchase: Failed to get original sku", ex);
			return new String();
		}
	}
}
