package com.fr.svnhooks;

import java.io.*;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DateTime: 14-3-11 9:41
 */
public class ConfigManager {
    private static final String DELIMITER = ",";
    private static Properties config;
    private static Properties reviewers;
    private static Properties reviewdb;

    static {
        config = loadProperties("config.properties");
        reviewers = loadProperties("reviewers.properties");
        reviewdb = loadProperties("reviewdb.properties");
    }

    public static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        try {
            InputStream in = new FileInputStream(new File(getFilePath(fileName)));
            properties.load(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static String getFilePath(String fileName) {
        return System.getProperty("user.dir") + "/" + fileName;
    }

    public static String getProperty(String key) {
        return config.getProperty(key);
    }

    public static String getReviewServer() {
        return config.getProperty("review_server");
    }

    public static String getReviewAdminUsername() {
        return config.getProperty("review_admin_username");
    }

    public static String getReviewAdminPassword() {
        return config.getProperty("review_admin_password");
    }

    public static String[] getSvnExperts() {
        return config.getProperty("svn_experts").split(DELIMITER);
    }

    public static String[] getReviewExperts() {
        return config.getProperty("review_experts").split(DELIMITER);
    }

    public static String[] getReviewPath() {
        return config.getProperty("review_path").split(DELIMITER);
    }

    public static String[] getIgnorePath() {
        return config.getProperty("ignore_path").split(DELIMITER);
    }

    public static int getMinNormalCount() {
        return Integer.parseInt(config.getProperty("min_shit_it_count"));
    }

    public static int getMinExpertCount() {
        return Integer.parseInt(config.getProperty("min_expert_ship_it_count"));
    }

    public static String[] getDefaultReviewers() {
        return config.getProperty("default_reviewers").split(DELIMITER);
    }

    public static String[] getMustReviewers(String submitter) {
        String reviews = reviewers.getProperty(submitter);
        if (reviews == null) {
            return getDefaultReviewers();
        }
        return reviews.split(DELIMITER);
    }

    public static boolean shouldCommitWithCustomTools() {
        String should = config.getProperty("commit_with_custom_tool");
        return "yes".equals(should);
    }

    public static String filePathCommitWithCustomTools() {
        return config.getProperty("commit_with_custom_tool_path");
    }

    public static int getMinReviewID() {
        String reviewID = config.getProperty("min_review_id");
        if (reviewID != null) {
            return Integer.parseInt(reviewID);
        }
        return 1;
    }



    public static void storage(int id) throws SvnException {
        if (id < getMinReviewID()) {
            throw new SvnException("Review id has been used!");
        }
        try {
            FileOutputStream out = new FileOutputStream(new File(getFilePath("reviewdb.properties")));
            reviewdb.setProperty(id + "", id + "");
            reviewdb.store(out, "Save " + id + " to db file");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        String filePath = "/Users/richie/Documents/develop/code/project/base/src/com/fr/abc.java";
        String reg = "/project.*/src";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.find()) {
            System.out.println("find project");
        } else {
            System.out.println("not found project");
        }
    }
}
