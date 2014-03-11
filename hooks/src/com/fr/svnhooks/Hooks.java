package com.fr.svnhooks;

import com.fr.svnhooks.json.JSONArray;
import com.fr.svnhooks.json.JSONObject;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: richie
 * Date: 13-8-21
 * Time: ÏÂÎç2:02
 */
public class Hooks {
    private static final String SVN_CMD = "svnlook";

    public static void main(String[] args) {
        String repos = args[0];
        String txn = args[1];

        try {
            String author = getAuthor(repos, txn);
            if (contains(ConfigManager.getSvnExperts(), author)) {
                return;
            }

            String changed = getChanged(repos, txn);
            String[] arr = changed.split("\n");
            for (String filePath : arr) {

                if (shouldIgnore(filePath)) {
                    continue;
                }

                for (String review_path : ConfigManager.getReviewPath()) {
                    Pattern pattern = Pattern.compile(review_path);
                    Matcher matcher = pattern.matcher(filePath);
                    if (matcher.find()) {
                        checkFiles(repos, txn, filePath, author);
                        break;
                    }
                }
            }
        } catch (SvnException e) {
            System.err.print(e.getMessage());
            System.exit(1);
        }

        System.exit(0);
    }

    private static boolean shouldIgnore(String filePath) {
        for (String path : ConfigManager.getIgnorePath()) {
            Pattern pattern = Pattern.compile(path);
            Matcher matcher = pattern.matcher(filePath);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }


    private static String getAuthor(String repos, String txn) throws SvnException {
        return getCmdOutput(new String[]{SVN_CMD, "author", "-t", txn, repos});

    }

    private static String getChanged(String repos, String txn) throws SvnException {
        return getCmdOutput(new String[]{SVN_CMD, "changed", "-t", txn, repos});
    }

    private static String getLog(String repos, String txn) throws SvnException {
        return getCmdOutput(new String[]{SVN_CMD, "log", "-t", txn, repos});
    }

    private static void checkFiles(String repos, String txn, String filePath, String author) throws SvnException {
        if (ConfigManager.shouldCommitWithCustomTools() && !isCommitFromCommitorTools(repos, txn, filePath)) {
            throw new SvnException("Please use commitor");
        }
        int reviewId = getReviewId(repos, txn);
        Opener opener = new Opener(ConfigManager.getReviewServer(),
                ConfigManager.getReviewAdminUsername(),
                ConfigManager.getReviewAdminPassword());
        try {
            JSONObject result = new JSONObject(opener.open(reviewId));

            if (!"ok".equalsIgnoreCase(result.getString("stat"))) {
                throw new SvnException("Get review info error!");
            }
            List<String> ship_it_users = new ArrayList<String>();
            JSONArray ja = result.getJSONArray("reviews");
            for (int i = 0; i < ja.length(); i++) {
                String title = ja.getJSONObject(i).getJSONObject("links").getJSONObject("user").getString("title");
                ship_it_users.add(title);
            }
            if (ship_it_users.size() < ConfigManager.getMinNormalCount()) {
                throw new SvnException("not enough of ship_it.");
            }
            int expertCount = 0;
            for (String ship : ship_it_users) {
                if (contains(ConfigManager.getReviewExperts(), ship)) {
                    expertCount++;
                }
            }
            if (expertCount < ConfigManager.getMinExpertCount()) {
                throw new SvnException("not enough of key user ship_it.");
            }

            String[] mustReviewer = ConfigManager.getMustReviewers(author);
            if (mustReviewer == null) {
                mustReviewer = ConfigManager.getDefaultReviewers();
            }

            int index = reviewIndex(ship_it_users, mustReviewer);

            if (index != -1) {
                throw new SvnException(mustReviewer[index] + " hasn't review!");
            }
            ConfigManager.storage(reviewId);
        } catch (Exception e) {
            throw new SvnException(e.getMessage());
        }
    }

    private static int reviewIndex(List<String> ship_it_users, String[] reviewers) {
        for (int i = 0; i < reviewers.length; i++) {
            if (!ship_it_users.contains(reviewers[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int getReviewId(String repos, String txn) throws SvnException {
        String log = getLog(repos, txn);
        Matcher matcher = Pattern.compile("^review:([0-9]+)").matcher(log);
        while (matcher.find()) {
            return Integer.parseInt(matcher.group().substring(7));
        }
        throw new SvnException("No review id");
    }

    private static String getCmdOutput(String[] cmd) throws SvnException {
        Process proc = null;
        try {
            String cmdString = join(Arrays.asList(cmd), " ");
            proc = Runtime.getRuntime().exec(cmdString);
        } catch (IOException e) {
            throw new SvnException(e.getMessage());
        }
        InputStream stdin = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdin);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new SvnException(e.getMessage());
        } finally {
            try {
                stdin.close();
                isr.close();
            } catch (IOException e) {

            }
        }
        return sb.toString().trim();
    }

    public static String join(java.util.Collection c, String se) {
        StringBuffer sb = new StringBuffer();
        java.util.Iterator it = c.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            sb.append(o);
            if (it.hasNext()) {
                sb.append(se);
            }
        }
        return sb.toString();
    }

    private static boolean contains(String[] arr, String ele) {
        return Arrays.asList(arr).contains(ele);
    }

    private static boolean isCommitFromCommitorTools(String repos, String txn, String filePath) throws SvnException {
        Pattern pattern = Pattern.compile(ConfigManager.filePathCommitWithCustomTools());
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.find()) {
            String log = getLog(repos, txn);
            if (log.contains(message())) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static String getMD5Str(String str) {
        MessageDigest messageDigest = null;

        try {
            messageDigest = MessageDigest.getInstance("MD5");

            messageDigest.reset();

            messageDigest.update(str.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException caught!");
            System.exit(-1);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        byte[] byteArray = messageDigest.digest();

        StringBuffer md5StrBuff = new StringBuffer();

        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));
            else
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
        }
        return md5StrBuff.toString();
    }

    public static String message() {
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date());
        return getMD5Str(dateString);
    }

}
