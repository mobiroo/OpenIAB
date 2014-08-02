package org.onepf.store;

import java.util.List;

import com.mobiroo.xgen.R;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;

public class StoreActivity extends Activity {
    private static final String TAG = StoreActivity.class.getSimpleName();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        List<PackageInfo> apps = getPackageManager().getInstalledPackages(0);
        Log.d(TAG, "onCreate() ***********   PACKAGES INFO   ***********");
        for (PackageInfo app : apps) {
            String installer = getPackageManager().getInstallerPackageName(app.packageName);
            Log.d(TAG, "package: " + app.packageName + " ; installer: " + installer);
        }
    }
}
