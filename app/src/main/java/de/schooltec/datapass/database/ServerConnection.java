package de.schooltec.datapass.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

class ServerConnection
{
    private static InputStream getInputStream(String urlStr)
            throws IOException, KeyManagementException, NoSuchAlgorithmException
    {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Create the SSL connection (Only used for HTTPSURLConnection)
        /*SSLContext sc;
        sc = SSLContext.getInstance("TLS");
        sc.init(null, null, new java.security.SecureRandom());
        conn.setSSLSocketFactory(sc.getSocketFactory());*/

        // set Timeout and method
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
        conn.setReadTimeout(7000);
        conn.setConnectTimeout(7000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        // Add any data you wish to post here
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.flush();
        writer.close();
        os.close();

        conn.connect();
        return conn.getInputStream();
    }

    String getStringFromUrl(String url) throws Exception
    {
        String result = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream(url)));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            result += inputLine;
        }
        return result;
    }
}
