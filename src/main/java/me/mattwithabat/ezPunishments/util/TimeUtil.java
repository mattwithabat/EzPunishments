package me.mattwithabat.ezPunishments.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return -1;

        Matcher matcher = TIME_PATTERN.matcher(input);
        long total = 0;

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            total += switch (unit) {
                case "s" -> value * 1000L;
                case "m" -> value * 60 * 1000L;
                case "h" -> value * 60 * 60 * 1000L;
                case "d" -> value * 24 * 60 * 60 * 1000L;
                case "w" -> value * 7 * 24 * 60 * 60 * 1000L;
                case "M" -> value * 30 * 24 * 60 * 60 * 1000L;
                case "y" -> value * 365 * 24 * 60 * 60 * 1000L;
                default -> 0L;
            };
        }

        return total > 0 ? total : -1;
    }

    public static String formatDuration(long millis) {
        if (millis < 0) return "Permanent";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + (hours % 24 > 0 ? " " + (hours % 24) + " hour" + (hours % 24 > 1 ? "s" : "") : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + (minutes % 60 > 0 ? " " + (minutes % 60) + " minute" + (minutes % 60 > 1 ? "s" : "") : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }

    public static String formatRemaining(long expiresAt) {
        if (expiresAt < 0) return "Permanent";
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        return formatDuration(remaining);
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        return sdf.format(new Date(timestamp));
    }
}
