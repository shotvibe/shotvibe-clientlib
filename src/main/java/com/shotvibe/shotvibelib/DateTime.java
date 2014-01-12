package com.shotvibe.shotvibelib;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTime {
    /***
     * Microseconds since the UNIX epoch: 1970-01-01 00:00
     */
    private long mTimeStamp;

    private DateTime(long timeStamp) {
        mTimeStamp = timeStamp;
    }

    public static DateTime FromTimeStamp(long timeStamp) {
        return new DateTime(timeStamp);
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public String formatISO8601() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        calendar.setTimeInMillis(getTimeStamp() / 1000L);

        int year = calendar.get(Calendar.YEAR);
        int javaMonth = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        int month = javaMonth + 1;

        long microseconds = getTimeStamp() % 1000000L;

        return String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d:%02d.%06dZ", year, month, day, hour, minute, second, microseconds);
    }

    private static Pattern parsePattern = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})([.,](\\d{1,6}))?Z"
    );

    public static DateTime ParseISO8601(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }

        Matcher m = parsePattern.matcher(input);
        if (!m.find()) {
            return null;
        }

        int year = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int day = Integer.parseInt(m.group(3));
        int hour = Integer.parseInt(m.group(4));
        int minute = Integer.parseInt(m.group(5));
        int second = Integer.parseInt(m.group(6));

        int javaMonth = month - 1;

        String fractionalSeconds = m.group(8);

        long microseconds;

        if (fractionalSeconds == null) {
            microseconds = 0;
        } else {
            microseconds = Integer.parseInt(fractionalSeconds);
            for (int i = 0; i < 6 - fractionalSeconds.length(); ++i) {
                microseconds *= 10;
            }
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setLenient(false);
        long epochSeconds;
        try {
            //noinspection MagicConstant
            calendar.set(year, javaMonth, day, hour, minute, second);
            epochSeconds = calendar.getTimeInMillis() / 1000L;
        } catch (IllegalArgumentException ex) {
            // calendar fields have invalid values. Not a valid date
            return null;
        }

        return new DateTime(epochSeconds * 1000000L + microseconds);
    }
}
