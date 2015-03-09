package org.onepf.oms.appstore.mobirooUtils;

import java.security.KeyStore;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class HttpUtils
{
    public static HttpClient getNewHttpClient(HttpParams reqParams)
    {
        try
        {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new EasySSLSocketFactory(trustStore);

            HttpParams params = new BasicHttpParams();
            if (reqParams == null)
            {
                HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            }
            else
            {
                HttpProtocolParams.setVersion(reqParams, HttpVersion.HTTP_1_1);
                HttpProtocolParams.setContentCharset(reqParams, HTTP.UTF_8);
            }

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            if (reqParams == null)
            {
            	ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
            	return new DefaultHttpClient(ccm, params);
            }
            else
            {
            	ClientConnectionManager ccm = new ThreadSafeClientConnManager(reqParams, registry);
            	return new DefaultHttpClient(ccm, reqParams);
            }
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }
    public static HttpClient getNewHttpClient()
    {
    	return getNewHttpClient(null);
    }
}
