package com.ntu.cloudgui.hostmanager.util;

/**
 * Simple logger helper (optional).
 * Can be replaced with java.util.logging or a real logging framework.
 */
public class LogUtil {

    public static void info(String msg) {
        System.out.println("[INFO] " + msg);
    }

    public static void error(String msg, Throwable t) {
        System.err.println("[ERROR] " + msg);
        if (t != null) {
            t.printStackTrace();
        }
    }
}
