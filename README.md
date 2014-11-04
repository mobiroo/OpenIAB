How To add OpenIAB into an app for the Mobiroo Appstore
=====
1. Download the latest version of OpenIAB.jar from the Mobiroo portal stie and attach it to the project.
Or clone the library `git clone https://github.com/mobiroo/OpenIAB.git` and add /library as a Library Project.

2. Map Google Play SKUs to Yandex/Amazon/etc SKUs like this:
https://github.com/mobiroo/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L109

3. Instantiate `new OpenIabHelper`  and call `helper.startSetup()`.
When setup is done call  `helper.queryInventory()`
    ```java
       helper = new OpenIabHelper(this, storeKeys);
       helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
           public void onIabSetupFinished(IabResult result) {
               if (!result.isSuccess()) {
                   complain("Problem setting up in-app billing: " + result);
                   return;
               }
               Log.d(TAG, "Setup successful. Querying inventory.");
               helper.queryInventoryAsync(mGotInventoryListener);
           }
       });
    ```
https://github.com/mobiroo/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L196

4. Handle the results of `helper.queryInventory()` in an inventory listener and update UI to show what was purchased
https://github.com/mobiroo/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L223

5. To process purchases you need to override `onActivityResult()` of your Activity
    ```java
       @Override
       protected void onActivityResult(int requestCode, int resultCode, Intent data) {
           // Pass on the activity result to the helper for handling
           mHelper.handleActivityResult(requestCode, resultCode, data));
       }
    ```
When the user requests purchase of an item, call  `helper.launchPurchaseFlow()`
https://github.com/mobiroo/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L294
and handle the results with the listener
https://github.com/mobiroo/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L396

6. If the user has purchased a consumable item, call  ``` helper.consume() ```
to exclude it from the inventory. If the item is not consumed, a store supposes it as non-consumable item and doesn't
allow to purchase it one more time. Also it will be returned by ``` helper.queryInventory() ``` next time
https://github.com/mobiroo/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L415

7. To help ensure the integrity of the transaction information that is sent to your application, Mobiroo Storefront
signs the JSON string that contains the response data for a purchase order. Mobiroo Storefront uses a private key to
create this signature. 

Add the following key to your application:

    ```
    final public String MOBIROO_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkd5P6tcVYNa4xFk"
            + "KHt592HlOYrFq9T1OlDvkFpkdN9o+0cvTF2iQ8fjPQJ7eG6cr7Hc7Ch0mRtr1g4LFLQI92OrLAGTCag5Uyk/z4u"
            + "KZUTtN/o54PSWMx16XiuzaK66MWmNfTnEVTb4uYXqnajZkNjfA6QdxT03c3zMLUwuf0kKwSaRjmWyY7Rb+mjRmx"
            + "f09FIktB9fZPg1LSXXISalW4SC5hH7CYPxca1NuJmLq1uN0ANRgchh6jVXOhb+IAp9BugjlJ0LAFzQU76mOHoon"
            + "yy/kD1abGo/WruKh8+LuCrjvV0p7EJxR0AZEFnGystjVLwLsMPhHs7q7Tp77NplhVwIDAQAB";
    ```

Register the public key with the OpenIabHelper:

    ```
    Map<String, String> storeKeys = new HashMap<String, String>();
    storeKeys.put(OpenIabHelper.NAME_MOBIROO, MOBIROO_PUB_KEY);

    Options options = new Options();
    options.storeKeys = storeKeys;
    options.verifyMode = Options.VERIFY_ONLY_KNOWN; //by setting this option the OpenIAB library will verify the
    // signatures only if publicKey is available. Otherwise it will skip the verification process.
 
    OpenIabHelper mHelper = new OpenIabHelper(this, options);
    ```

When your application receives this signed response you can use mobiroo public key to verify the signature. By
performing signature verification you can detect responses that have been tampered with or that have been spoofed.
You can perform this signature verification step in your application; however, if your application connects to a
secure remote server then we recommend that you perform the signature verification on that server.

    ```
    // on your purchase finished listener:
    public void onIabPurchaseFinished(IabResult result, Purchase purchase)
    {
        String origJson = purchase.getOriginalJson();//original purhcase JSON data as received from the server.
        String signature = purchase.getSignature();//the signed json data, data will be signed by the server using mobiroo private key.
        boolean valid = org.onepf.oms.appstore.googleUtils.Security.verifyPurchase(MOBIROO_PUB_KEY, origJson, signature);
        if(valid) Log.d("Signature is valid");
        else Log.w("Purchase/Signature is NOT valid");
    }
    ```

8. Add the required permissions to the AndroidManifest.xml

    ```xml
    <!-- If you are supporting just the Mobiroo Appstore... -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="org.onepf.openiab.permission.BILLING" />

    <!--
    If you need to support more than just Mobiroo's Appstore, add the required permissions as indicated by the XML comments...
    -->

    <!--all-->
    <uses-permission android:name="android.permission.INTERNET"/>
    <!--Google Play-->
    <uses-permission android:name="com.android.vending.BILLING" />
    <!--Open Store-->
    <uses-permission android:name="org.onepf.openiab.permission.BILLING" />
    <!--Amazon-->
    <!--Amazon requires no permissions -->
    <!--Samsung Apps-->
    <uses-permission android:name="com.sec.android.iap.permission.BILLING" />
    <!--Fortumo-->
    <uses-permission android:name="android.permission.RECEIVE_SMS"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <!--SlideME-->
    <uses-permission android:name="com.slideme.sam.manager.inapp.permission.BILLING" />

    <!--
    Be careful using sms permissions. If you want to support devices without sms functionality, don't forget to add
    -->

      <uses-feature android:name="android.hardware.telephony" android:required="false"/>
    ```

9. Mobiroo does not require any modifications to your proguard configuration, However if you need to support more than
just the Mobiroo Appstore; edit your proguard config file as follows:

    ```
    # GOOGLE
    -keep class com.android.vending.billing.**

    # AMAZON
    -dontwarn com.amazon.**
    -keep class com.amazon.** {*;}
    -keepattributes *Annotation*
    -dontoptimize

    # SAMSUNG
    -keep class com.sec.android.iap.**
    
    # NOKIA
    -keep class com.nokia.payment.iap.aidl.**

    #FORTUMO
    -keep class mp.** { *; }
    ```

10. Troubleshooting: additional logging is very helpful if you trying to understand what's wrong with configuration or raise issue:

    ```java
    helper.enableDebugLogging(true);
    ```

11. To test .apk with Mobiroo OpenIAB Tester some steps are needed:

    - Download and install Mobiroo OpenIAB Tester from the Mobiroo portal site
    - Download JSON with in-app products from the Mobiroo portal site and put JSON with in-app products to /mnt/sdcard
    - Install your .apk with special option to help OpenIAB choose Amazon protocol
    ```bash
    # install for Mobiroo OpenIAB Tester:
    adb install -i com.mobiroo.xgen /path/to/YourApp.apk
    ```

Receipt Verification on Server
---------------------

1. The Mobiroo OpenIAB project defaults to having the verifyMode option set to VERIFY_ONLY_KNOWN. If you are using the OnePF version
stop now and switch to using the Mobiroo version of the OpenIAB library instead. Do not use the OnePF version of the OpenIAB library
when connecting to the Mobiroo storefronts. At this point you need to specify

    ```java
    Options opts = new OpenIabHelper.Options();
    opts.verifyMode = Options.VERIFY_ONLY_KNOWN;
    mHelper = new OpenIabHelper(context, opts);
    ```

2. The Mobiroo Appstore has multiple implementations, one for each channel partner that Mobiroo deals with. Due to this situation, for
all intensive purposes; Mobiroo actually is a combination of many "island" Appstores under one umbrella. However, due to contractual and
security agreements; all channel Appstores are segregated and independent of eachother. This complicates the Receipt Verification process
because there are multiple domains and each supports it's own Receipt Verification service. In order to determine the correct channel,
Mobiroo storefront will add the channel ID as as an extra String data to the result intent of the purchase activity under the "CHANNEL_ID"
key. Developers are advised to override the onActivityResult method and extract the channel ID from the result intent.

    ```
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //please check your request code first
        String channelId = data.getStringExtra("CHANNEL_ID");
        Log.d("Channel ID = " + channelId);
    }
    ```

 The actual Receipt Verification service endpoint is constructed as follows:

    ```
    https://{channelname}.mobileplatform.solutions/api/v1.0/openiab/verify/{packagename}/inapp/{sku}/purchases/{token}
    ```

Where:

* **{channelname}** is replaced with the string received in onActivityResult().
* **{packagename}** is replaced with the package name of your application.
* **{sku}** is replaced with the Mobiroo Appstore SKU as entered into the Mobiroo portal site.
* **{token}** is replaced with the transaction token received with the Purchase record.

3. Get receipt's data and signature from Purchase object and send it to your server

    ```java
    new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            // ... different result checks ...
            String receiptData = purchase.getOriginalJson();
            String receiptSignature = purchase.getSignature();
            String storeName = purchase.getAppstoreName();
            String urlToContent  = yourRequestReceiptVerificationOnServer(receiptData, receiptSignature, storeName);
            // ... further code ...
        }
    }
    ```

Unity Plugin
=====
There is also Unity engine [plugin](unity_plugin) that will simplify integration for C#/JavaScript developers. No need to write any java code.

OpenIAB - Open In-App Billing
=====
Uploading Android apps to all the existing Android appstores is a painful process and [AppDF](/onepf/AppDF)
project was designed to make it easier. But what is even more difficult for the developers is
supporting different in-purchase APIs of different appstores. There are five different In-App Purchase APIs
already and this number is increasing. We are going to create an open source library that will wrap
appstore in-app purchase APIs of all the stores and provide an easy way for the developers to develop
their apps/games in a way that one APK will work in all the stores and automatically use right in-app
purchase API under each store. Plus we are going to develop an open in-app billing API that stores
could implement to support all the built APK files using this library.

How OpenIAB Works
=====
1. An Android app developer integrates OpenIAB library in his/her Android code
2. An Android app developer implements in-app purchases using OpenIAB API (which is very close to Google Play IAB API, just few changes in source code will be needed)
3. OpenIAB Lib detects which appstore installed the app
4. OpenIAB Lib redirects in-app purchase calls to the corresponding appstore IAB API (OpenIAB Lib wrapps IAB APIs of severall apstores)
5. All In-App Billing logic is handled by the corresponding appstore, OpenIAB has no code to process in-app purchases and has no UI, it just wrapps In-App Billing APIs of different stores in one library

<img src="http://www.onepf.org/img/openiabdiagram1.png">

<img src="http://www.onepf.org/img/openiabdiagram2.png">

Current Status
=====
OpenIAB SDK is used in production by wide variety of application and games. OpenIAB packages are available for Android apps and games based on Unity3D or Marmalade SDK. OpenIAB protocol is implemented by several Appstores.

We have some samples that works in any Appstore in our [samples folder](https://github.com/mobiroo/OpenIAB/tree/master/samples). To find differences between TrivialDrive provided by Google and TrivialDrive with OpenIAB, please check our [sample](https://github.com/mobiroo/OpenIAB/tree/master/samples/trivialdrive). It demonstrates what changes need to be done to work with all Appstores and Carrier Billing.

If you are an Appstore developer and want to know how to integrate OpenIAB protocol in your Appstore, please start with our [Step-By-Step How-To](https://github.com/mobiroo/OpenIAB/blob/master/specification/How-to_Implement_OpenIAB_in_Appstore.md)

Basic Principles
=====
* **As close to Google Play In-app Billing API as possible** - we optimize the OpenIAB library by the following parameter "lines on code you need to change in an app that already works in Google Play to make it working in all the appstores"
* **One APK works in all appstores** - OpenIAB chooses proper billing method automatically or follows your requirements
* **Open In-App Billing protocol** - OpenIAB is designed provide lightweight solution that supports hundreds of appstores. When appstore implement OpenIAB protocol on appstore side all applications with OpenIAB become fully compatible with new appstore without recompile.
* **No middle man**

No Middle Man
=====
OpenIAB is an open source library that handles OpenIAB protocol and wraps some already existing IAB SDKs as well.
It is important to understand that all payments are processed directly by appstore and there is no a middle man
staying between the app developers and the appstores.
OpenIAB is not a payment service. It is just an API how the apps communicate with appstores to request in-app billing.
There is a common open API all the stores can use instead of each new store implement their own API
and developers have to integrate all these different APIs in their apps.


How Can I Help?
=====

* If you know about issues we missed - please, let us know in <a href="https://github.com/mobiroo/OpenIAB/issues">Issues on GitHub</a>
* If you have contacts with Appstore you like, ask them to implement <a href="https://github.com/mobiroo/OpenIAB/blob/master/specification/How-to_Implement_OpenIAB_in_Appstore.md">OpenIAB</a> on their side
* If you are an Android app developer check <a href="https://github.com/mobiroo/OpenIAB/issues?state=open">the list of open tasks</a>, see if any of these tasks interests you and comment it. <a href="https://github.com/mobiroo/OpenIAB">Fork OpenIAB</a> on GitHub and submit your code</li>
* If you are an Appstore and already support In-App Billing we will be happy to meet with your API and find best way to make it compatible with OpenIAB. Please, raise an <a href="https://github.com/mobiroo/OpenIAB/issues?state=open">Issue</a> to let us know</li>
* If you are an appstore that does not yet support in-app billing, but plans to support it, then we will be glad to help you with OpenIAB API. Please check our <a href="https://github.com/mobiroo/OpenIAB/blob/master/specification/How-to_Implement_OpenIAB_in_Appstore.md">How-To</a> and contact us to get deeper explanation of questions you have by raising an <a href="https://github.com/mobiroo/OpenIAB/issues?state=open">Issue</a></li>


Why did Mobiroo completely fork the OpenIAB project?
=====

There are changes that needed to be made to the OpenIAB project in order to fully support Mobiroo's IAB services. In addition to
these changes (which will be submitted to the upstream OpenIAB project in due time), Mobiroo found that the 3rd party app developers
wishing to support Mobiroo's IAB implementation were getting rather confused with the documentation being generic in order to
support all known Appstores. In that light, Mobiroo has opted to completely fork the OpenIAB library (at tagged version 0.9.6.1) and
is in the process of re-writing all documentation, sample apps and the local testing store for specific use by Mobiroo 3rd party
developers.

Mobiroo pledges to maintain this repository in perpetuity and to push all useful changes back to the main OnePF OpenIAB project in due time.

License
=====
Source code of the OpenIAB library and the samples is available under the terms of the Apache License, Version 2.0:
http://www.apache.org/licenses/LICENSE-2.0

The OpenIAB API specification and the related texts are available under the terms of the Creative Commons Attribution 2.5 license:
http://creativecommons.org/licenses/by/2.5/
