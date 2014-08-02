package org.onepf.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.onepf.store.data.Database;

import com.mobiroo.xgen.R;

import android.app.Application;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

public class StoreApplication extends Application {

    public static final String TAG = "mobiroo-store"; //< Mobiroo: Changed from OpenPF-store
    static final String GOOGLE_CONFIG_FILE = "google-play.csv";
    static final String AMAZON_CONFIG_FILE = "amazon.sdktester.json";
    static final String ONEPF_CONFIG_FILE = "onepf.xml";
    static final String MOBIROO_CONFIG_FILE = "mobiroo.xml"; //< Mobiroo: Added

    Database _database;
    FileObserver _configObserver;

    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        copyConfigFromAssets(GOOGLE_CONFIG_FILE);
        copyConfigFromAssets(AMAZON_CONFIG_FILE);
        copyConfigFromAssets(ONEPF_CONFIG_FILE);
        copyConfigFromAssets(MOBIROO_CONFIG_FILE); //< Mobiroo: Added

        if (createDbFromConfig()) {
            _configObserver = new FileObserver(getConfigDir()) {
                @Override
                public void onEvent(int event, String file) {
                    switch (event) {
                        case FileObserver.CLOSE_WRITE:
                            createDbFromConfig();
                            break;
                    }
                }
            };
            _configObserver.startWatching();
        }
    }

    private void copyConfigFromAssets(String configFile) {
        File configDir = new File(getConfigDir());
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                Log.e(TAG, this.getString(R.string.cfg_dir_fail)); //< Mobiroo: translations
                return;
            }
        }

        File outFile = new File(getConfigDir(), configFile);
        if (outFile.exists()) {
            return;
        }

        InputStream in;
        OutputStream out;
        try {
            in = getAssets().open(configFile);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
        } catch(IOException e) {
            Log.e(TAG, this.getString(R.string.cfg_copy_fail, configFile), e); //< Mobiroo: translations
        }
    }

    private String getConfigDir() {
        return Environment.getExternalStorageDirectory() + File.separator + "mobiroo-store"; //< Mobiroo: changed from "OnePF-store"
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private boolean createDbFromConfig() {
        try {
            _database = new Database(this);
            _database.deserializeFromGoogleCSV(readConfigFromSdCard(GOOGLE_CONFIG_FILE));
            _database.deserializeFromAmazonJson(readConfigFromSdCard(AMAZON_CONFIG_FILE));
            _database.deserializeFromOnePFXML(readConfigFromSdCard(ONEPF_CONFIG_FILE));
            _database.deserializeFromOnePFXML(readConfigFromSdCard(MOBIROO_CONFIG_FILE)); //< Mobiroo: Added
        } catch (Exception e) {
            Log.e(TAG, this.getString(R.string.cfg_parse_fail), e); //< Mobiroo: translations
            _database = new Database(this);
            return false;
        }
        return true;
    }

    private String readConfigFromSdCard(String configFile) {
        File file = new File(getConfigDir(), configFile);
        if (!file.exists()) {
            Log.i(TAG, this.getString(R.string.cfg_file_not_found)); //< Mobiroo: translations
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String temp;
            while ((temp = br.readLine()) != null) {
                sb.append(temp).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, this.getString(R.string.cfg_read_fail), e); //< Mobiroo: translations
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(TAG, this.getString(R.string.cfg_read_close_fail), e); //< Mobiroo: translations
            }
        }
        return sb.toString();
    }
}
