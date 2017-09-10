package com.gimo.robot.qq.web.util;

public class StringUtils {

	public static boolean contains(final String string, final String[] strings) {
        if (null == strings) {
            return false;
        }
        
        for (final String str : strings) {
            if (null == str && null == string) {
                return true;
            }

            if (null == string || null == str) {
                continue;
            }

            if (string.equals(str)) {
                return true;
            }
        }

        return false;
    }
	
	public static String[] trimAll(final String[] strings) {
        if (null == strings) {
            return null;
        }

        final String[] ret = new String[strings.length];

        for (int i = 0; i < strings.length; i++) {
            ret[i] = strings[i].trim();
        }

        return ret;
    }
}
