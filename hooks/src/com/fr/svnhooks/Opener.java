package com.fr.svnhooks;

import sun.misc.BASE64Encoder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created with IntelliJ IDEA.
 * User: richie
 * Date: 13-8-21
 * Time: ÏÂÎç11:24
 */
public class Opener {
    private String server;
    private String username;
    private String password;

    public Opener(String server, String username, String password) {
        this.server = server;
        this.username = username;
        this.password = password;
    }

    public String open(int reviewID) throws Exception {
        URL url = new URL(server + "/api/review-requests/" + reviewID + "/reviews/");
        URLConnection urlConnection = url.openConnection();
        String auth = username + ":" + password;

        String authStringEnc = new BASE64Encoder().encode(auth.getBytes());
        ;
        urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        InputStream is = urlConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);

        int numCharsRead;
        char[] charArray = new char[1024];
        StringBuffer sb = new StringBuffer();
        while ((numCharsRead = isr.read(charArray)) > 0) {
            sb.append(charArray, 0, numCharsRead);
        }
        return sb.toString();
    }
}
