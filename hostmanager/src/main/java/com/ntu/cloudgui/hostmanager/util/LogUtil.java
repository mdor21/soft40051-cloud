package com.ntu.cloudgui.hostmanager.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A utility class for logging.
 */
public class LogUtil {

    private static final Logger logger = LogManager.getLogger(LogUtil.class);

    public static void info(String message) {
        logger.info(message);
    }

    public static void error(String message, Throwable t) {
        logger.error(message, t);
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void debug(String message) {
        logger.debug(message);
    }
}
