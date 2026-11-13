package com.marosirobi.workouttrackerapi.utils;

public final class LogUtils {

    private LogUtils() { }

    // "user@gmail.com" -> "us***@gmail.com"
    public static String maskEmail(String email) {
        if (email == null) return "-";

        String clean = email.trim().replaceAll("[\\r\\n]", "");
        int at = clean.indexOf('@');
        if (at <= 0) return "***";

        String local = clean.substring(0, at);
        int visible = local.length() > 2 ? 2 : 1;
        return local.substring(0, visible) + "***" + clean.substring(at);
    }
}
