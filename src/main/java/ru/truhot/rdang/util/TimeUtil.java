package ru.truhot.rdang.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    public static long parse(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 10L;

        long totalSeconds = 0;
        Pattern pattern = Pattern.compile("(\\d+)([dhms])");
        Matcher matcher = pattern.matcher(timeStr.toLowerCase());

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d" -> totalSeconds += value * 86400;
                case "h" -> totalSeconds += value * 3600;
                case "m" -> totalSeconds += value * 60;
                case "s" -> totalSeconds += value;
            }
        }
        return totalSeconds > 0 ? totalSeconds : 10L;
    }

    public static String format(long seconds) {
        if (seconds < 60) return seconds + "с";
        long m = seconds / 60;
        long s = seconds % 60;
        if (m < 60) return m + "м " + s + "с";
        long h = m / 60;
        m = m % 60;
        return h + "ч " + m + "м " + s + "с";
    }
}