package com.gimo.robot.qq.web.util;

import java.util.ResourceBundle;


public class PropReader {

    private static final ResourceBundle CFG = ResourceBundle.getBundle("robot");

    public static final String QQ_BOT_NAME = PropReader.getString("qq.bot.name");

    public static String getString(final String key) {
        return CFG.getString(key);
    }

    public static Boolean getBoolean(final String key) {
        final String stringValue = getString(key);

        if (null == stringValue) {
            return null;
        }

        return Boolean.valueOf(stringValue);
    }

    public static Float getFloat(final String key) {
        final String stringValue = getString(key);
        if (null == stringValue) {
            return null;
        }

        return Float.valueOf(stringValue);
    }

    public static Integer getInt(final String key) {
        final String stringValue = getString(key);
        if (null == stringValue) {
            return null;
        }

        return Integer.valueOf(stringValue);
    }

    public static Long getLong(final String key) {
        final String stringValue = getString(key);
        if (null == stringValue) {
            return null;
        }

        return Long.valueOf(stringValue);
    }

    private PropReader() {

    }
}
