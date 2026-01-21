package com.bharatshop.logging;

import java.util.Arrays;

public class ArgSanitizer {
    public static String sanitize(Object arg) {
        if (arg == null) return "null";
        String s;
        try {
            s = String.valueOf(arg);
        } catch (Exception e) {
            s = arg.getClass().getName();
        }
        if (s.length() > 500) {
            return s.substring(0, 500) + "...";
        }
        return s;
    }

    public static String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        String[] parts = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            parts[i] = "arg" + i + "=" + sanitize(args[i]);
        }
        return Arrays.stream(parts).reduce((a, b) -> a + ", " + b).orElse("");
    }
}