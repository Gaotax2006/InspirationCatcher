package com.inspiration.catcher.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static String formatDateTime(LocalDateTime dateTime) {return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);}
    public static String formatDate(LocalDateTime dateTime) {return dateTime == null ? "" : dateTime.format(DATE_FORMATTER);}
    public static String formatTime(LocalDateTime dateTime) {return dateTime == null ? "" : dateTime.format(TIME_FORMATTER);}
    public static String getRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        long diffInMinutes = java.time.Duration.between(dateTime, LocalDateTime.now()).toMinutes();
        return diffInMinutes < 1 ? "刚刚" : diffInMinutes < 60 ? diffInMinutes + "分钟前" : diffInMinutes < 1440 ? (diffInMinutes / 60) + "小时前" : diffInMinutes < 10080 ? (diffInMinutes / 1440) + "天前" : formatDateTime(dateTime);
    }
}