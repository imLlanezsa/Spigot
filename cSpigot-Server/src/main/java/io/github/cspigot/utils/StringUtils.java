package io.github.cspigot.utils;

import io.github.cspigot.config.SettingsConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class StringUtils {

    public static String PREFIX = CC.translate("&8[" + SettingsConfig.THEME + "cSpigot" + "&8] ");

    public static String repeat(String string, int count) {
        return new String(new char[count]).replace("\0", string);
    }

    public static String compile(Object[] objs) {
        return compile(objs, " ");
    }

    public static String compile(Object[] objs, String separator) {
        StringBuilder builder = new StringBuilder();
        for ( Object s : objs ) {
            if (builder.length() != 0) builder.append(separator);
            builder.append(s.toString());
        }
        return builder.toString();
    }

    public static String compile(int[] ints, String separator) {
        StringBuilder builder = new StringBuilder();
        for ( Object s : ints ) {
            if (builder.length() != 0) builder.append(separator);
            builder.append(s.toString());
        }
        return builder.toString();
    }

    public static String compile(Object[] objs, String separator, int start) {
        StringBuilder builder = new StringBuilder();
        for ( int i = start; i < objs.length; i++ ) {
            Object s = objs[i];
            if (builder.length() != 0) builder.append(separator);
            builder.append(s.toString());
        }
        return builder.toString();
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Hashing Failed for (" + input + ")");
    }
}
