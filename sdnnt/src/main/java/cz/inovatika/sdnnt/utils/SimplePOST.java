package cz.inovatika.sdnnt.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SimplePOST {


    public static Pair<Integer, String> post (String u, String payload) throws IOException {
        URL url = new URL( u );
        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "POST" );
        //conn.setRequestProperty( "Content-Type", "application/json");
        conn.setRequestProperty("Content-Type", "application/xml; utf-8");
        //conn.setRequestProperty( "Content-Length", Integer.toString( payload.length() ));
        conn.setUseCaches( false );
        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        int responseCode = conn.getResponseCode();
        String s = null;
        if (responseCode  == 200) {
            s = IOUtils.toString(conn.getInputStream(), "UTF-8");
        } else {
            s = IOUtils.toString(conn.getErrorStream(), "UTF-8");
        }
        return Pair.of(responseCode, s);

    }

    public static Pair<Integer, String> postJSON (String u, String payload) throws IOException {
        URL url = new URL( u );
        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setInstanceFollowRedirects( false );
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/json");
        //conn.setRequestProperty("Content-Type", "application/xml; utf-8");
        //conn.setRequestProperty( "Content-Length", Integer.toString( payload.length() ));
        conn.setUseCaches( false );
        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        int responseCode = conn.getResponseCode();
        String s = null;
        if (responseCode  == 200) {
            s = IOUtils.toString(conn.getInputStream(), "UTF-8");
        } else {
            s = IOUtils.toString(conn.getErrorStream(), "UTF-8");
        }
        return Pair.of(responseCode, s);

    }
}
