/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.date;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.runtime.Realm;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.3 Date Objects</h2>
 * <ul>
 * <li>20.3.1 Overview of Date Objects and Definitions of Abstract Operations
 * </ul>
 */
final class DateAbstractOperations {
    private DateAbstractOperations() {
    }

    /**
     * 5.2 Algorithm Conventions
     * 
     * @param dividend
     *            the dividend
     * @param divisor
     *            the divisor
     * @return the modulo result
     */
    private static final double modulo(double dividend, double divisor) {
        assert divisor != 0 && isFinite(divisor);
        double remainder = dividend % divisor;
        // NB: add +0 to convert -0 to +0
        return (remainder >= 0 ? remainder + (+0d) : remainder + divisor);
    }

    /**
     * 20.3.1.2 Day Number and Time within Day
     */
    public static final double msPerDay = 86400000;

    /**
     * 20.3.1.2 Day Number and Time within Day
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the number of days since the epoch
     */
    public static double Day(double t) {
        return Math.floor(t / msPerDay);
    }

    /**
     * 20.3.1.2 Day Number and Time within Day
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the number of milli-seconds since midnight
     */
    public static double TimeWithinDay(double t) {
        return modulo(t, msPerDay);
    }

    /**
     * 20.3.1.3 Year Number
     * 
     * @param y
     *            the year
     * @return the number of days
     */
    public static double DaysInYear(double y) {
        if (y % 4 != 0) {
            return 365;
        }
        if (y % 100 != 0) {
            return 366;
        }
        if (y % 400 != 0) {
            return 365;
        }
        return 366;
    }

    /**
     * 20.3.1.3 Year Number
     * 
     * @param y
     *            the year
     * @return the number of days since the epoch
     */
    public static double DayFromYear(double y) {
        return 365 * (y - 1970) + Math.floor((y - 1969) / 4) - Math.floor((y - 1901) / 100)
                + Math.floor((y - 1601) / 400);
    }

    /**
     * 20.3.1.3 Year Number
     * 
     * @param y
     *            the year
     * @return the number of milli-seconds since the epoch
     */
    public static double TimeFromYear(double y) {
        return msPerDay * DayFromYear(y);
    }

    /**
     * 20.3.1.3 Year Number
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the year
     */
    public static double YearFromTime(double t) {
        double y = 1970 + Math.floor(t / (365 * msPerDay));
        if (TimeFromYear(y) > t) {
            do {
                y -= 1;
            } while (TimeFromYear(y) > t);
        } else if (TimeFromYear(y) + DaysInYear(y) * msPerDay <= t) {
            do {
                y += 1;
            } while (TimeFromYear(y) + DaysInYear(y) * msPerDay <= t);
        }
        return y;
    }

    /**
     * 20.3.1.3 Year Number
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return {@code true} if the year is a leap year
     */
    public static boolean InLeapYear(double t) {
        return DaysInYear(YearFromTime(t)) == 366;
    }

    /**
     * 20.3.1.4 Month Number
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the month
     */
    public static double MonthFromTime(double t) {
        double d = DayWithinYear(t);
        double leap = InLeapYear(t) ? 1 : 0;

        if (0 <= d && d < 31) {
            return 0;
        }
        if (31 <= d && d < 59 + leap) {
            return 1;
        }
        if (59 + leap <= d && d < 90 + leap) {
            return 2;
        }
        if (90 + leap <= d && d < 120 + leap) {
            return 3;
        }
        if (120 + leap <= d && d < 151 + leap) {
            return 4;
        }
        if (151 + leap <= d && d < 181 + leap) {
            return 5;
        }
        if (181 + leap <= d && d < 212 + leap) {
            return 6;
        }
        if (212 + leap <= d && d < 243 + leap) {
            return 7;
        }
        if (243 + leap <= d && d < 273 + leap) {
            return 8;
        }
        if (273 + leap <= d && d < 304 + leap) {
            return 9;
        }
        if (304 + leap <= d && d < 334 + leap) {
            return 10;
        }
        if (334 + leap <= d && d < 365 + leap) {
            return 11;
        }
        return Double.NaN;
    }

    /**
     * 20.3.1.4 Month Number
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the number of days since new year
     */
    public static double DayWithinYear(double t) {
        return Day(t) - DayFromYear(YearFromTime(t));
    }

    /**
     * 20.3.1.5 Date Number
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the number of days since the epoch
     */
    public static double DateFromTime(double t) {
        double m = MonthFromTime(t);
        if (Double.isNaN(m)) {
            return Double.NaN;
        }
        assert (int) m == m && m >= 0 && m <= 11;
        double leap = InLeapYear(t) ? 1 : 0;
        switch ((int) m) {
        case 0:
            return DayWithinYear(t) + 1;
        case 1:
            return DayWithinYear(t) - 30;
        case 2:
            return DayWithinYear(t) - 58 - leap;
        case 3:
            return DayWithinYear(t) - 89 - leap;
        case 4:
            return DayWithinYear(t) - 119 - leap;
        case 5:
            return DayWithinYear(t) - 150 - leap;
        case 6:
            return DayWithinYear(t) - 180 - leap;
        case 7:
            return DayWithinYear(t) - 211 - leap;
        case 8:
            return DayWithinYear(t) - 242 - leap;
        case 9:
            return DayWithinYear(t) - 272 - leap;
        case 10:
            return DayWithinYear(t) - 303 - leap;
        case 11:
            return DayWithinYear(t) - 333 - leap;
        }
        return Double.NaN;
    }

    /**
     * 20.3.1.6 Week Day
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the week day
     */
    public static double WeekDay(double t) {
        return modulo(Day(t) + 4, 7);
    }

    /**
     * 20.3.1.7 Local Time Zone Adjustment
     * 
     * @param realm
     *            the realm instance
     * @return the locale time zone offset
     */
    public static double LocalTZA(Realm realm) {
        return TimeZoneInfo.getDefault().getRawOffset(realm.getTimeZone());
    }

    /**
     * 20.3.1.8 Daylight Saving Time Adjustment
     * 
     * @param realm
     *            the realm instance
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the day light saving time in milli-seconds
     */
    public static double DaylightSavingTA(Realm realm, double t) {
        if (Double.isNaN(t)) {
            return t;
        }
        assert Math.abs(t) <= 8.64e15;
        return TimeZoneInfo.getDefault().getDSTSavings(realm.getTimeZone(), (long) t);
    }

    /**
     * 20.3.1.9 Local Time
     * 
     * @param realm
     *            the realm instance
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the local time value in milli-seconds
     */
    public static double LocalTime(Realm realm, double t) {
        // return t + LocalTZA(realm) + DaylightSavingTA(realm, t);
        if (Double.isNaN(t)) {
            return t;
        }
        assert Math.abs(t) <= 8.64e15;
        return TimeZoneInfo.getDefault().localTime(realm.getTimeZone(), (long) t);
    }

    /**
     * 20.3.1.9 Local Time
     * 
     * @param realm
     *            the realm instance
     * @param t
     *            the local time value in milli-seconds
     * @return the date in milli-seconds since the epoch
     */
    public static double UTC(Realm realm, double t) {
        // double d = t - LocalTZA(realm);
        // return d - DaylightSavingTA(realm, d - realm.getTimezone().getDSTSavings());
        // TODO: spec issue
        // https://code.google.com/p/v8/issues/detail?id=3116
        // https://code.google.com/p/v8/issues/detail?id=3637
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1084434
        // return t - LocalTZA(realm) - DaylightSavingTA(realm, t - LocalTZA(realm));
        if (Double.isNaN(t) || Math.abs(t) > (8.64e15 + 8.64e7)) {
            return t;
        }
        return TimeZoneInfo.getDefault().utc(realm.getTimeZone(), (long) t);
    }

    /**
     * 20.3.1.10 Hours, Minutes, Second, and Milliseconds
     */
    public static final double //
            HoursPerDay = 24, //
            MinutesPerHour = 60, //
            SecondsPerMinute = 60, //
            msPerSecond = 1000, //
            msPerMinute = msPerSecond * SecondsPerMinute, //
            msPerHour = msPerMinute * MinutesPerHour;

    /**
     * 20.3.1.10 Hours, Minutes, Second, and Milliseconds
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the hours
     */
    public static double HourFromTime(double t) {
        return modulo(Math.floor(t / msPerHour), HoursPerDay);
    }

    /**
     * 20.3.1.10 Hours, Minutes, Second, and Milliseconds
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the minutes
     */
    public static double MinFromTime(double t) {
        return modulo(Math.floor(t / msPerMinute), MinutesPerHour);
    }

    /**
     * 20.3.1.10 Hours, Minutes, Second, and Milliseconds
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the seconds
     */
    public static double SecFromTime(double t) {
        return modulo(Math.floor(t / msPerSecond), SecondsPerMinute);
    }

    /**
     * 20.3.1.10 Hours, Minutes, Second, and Milliseconds
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the milli-seconds
     */
    public static double msFromTime(double t) {
        return modulo(t, msPerSecond);
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    /**
     * 20.3.1.11 MakeTime (hour, min, sec, ms)
     * 
     * @param hour
     *            the hour
     * @param min
     *            the minutes
     * @param sec
     *            the seconds
     * @param ms
     *            the milli-seconds
     * @return the date in milli-seconds
     */
    public static double MakeTime(double hour, double min, double sec, double ms) {
        if (!isFinite(hour) || !isFinite(min) || !isFinite(sec) || !isFinite(ms)) {
            return Double.NaN;
        }
        double h = ToInteger(hour);
        double m = ToInteger(min);
        double s = ToInteger(sec);
        double milli = ToInteger(ms);
        double t = h * msPerHour + m * msPerMinute + s * msPerSecond + milli;
        return t;
    }

    /**
     * 20.3.1.12 MakeDay (year, month, date)
     * 
     * @param year
     *            the year
     * @param month
     *            the month
     * @param date
     *            the date
     * @return the number of days
     */
    public static double MakeDay(double year, double month, double date) {
        if (!isFinite(year) || !isFinite(month) || !isFinite(date)) {
            return Double.NaN;
        }
        double y = ToInteger(year);
        double m = ToInteger(month);
        double dt = ToInteger(date);
        double ym = y + Math.floor(m / 12);
        double mn = modulo(m, 12);

        double[] monthStart = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
        double day = Math.floor(TimeFromYear(ym) / msPerDay) + monthStart[(int) mn];
        if (mn >= 2 && DaysInYear(ym) == 366) {
            day += 1;
        }

        return day + dt - 1;
    }

    /**
     * 20.3.1.13 MakeDate (day, time)
     * 
     * @param day
     *            the days
     * @param time
     *            the time in milli-seconds
     * @return the date in milli-seconds
     */
    public static double MakeDate(double day, double time) {
        if (!isFinite(day) || !isFinite(time)) {
            return Double.NaN;
        }
        return day * msPerDay + time;
    }

    /**
     * 20.3.1.14 TimeClip (time)
     * 
     * @param time
     *            the time in milli-seconds
     * @return the clipped time in milli-seconds
     */
    public static double TimeClip(double time) {
        if (!isFinite(time)) {
            return Double.NaN;
        }
        if (Math.abs(time) > 8.64e15) {
            return Double.NaN;
        }
        return ToInteger(time) + (+0d);
    }

    private static int DaysInMonth(int year, int month) {
        // month is 1-based for DaysInMonth!
        if (month == 2)
            return DaysInYear(year) == 366 ? 29 : 28;
        return month >= 8 ? 31 - (month & 1) : 30 + (month & 1);
    }

    /**
     * 20.3.1.15 Date Time String Format<br>
     * Parses the input string according to the simplified ISO-8601 Extended Format:
     * <ul>
     * <li><code>YYYY-MM-DD'T'HH:mm:ss.sss'Z'</code></li>
     * <li>or <code>YYYY-MM-DD'T'HH:mm:ss.sss[+-]hh:mm</code></li>
     * </ul>
     * 
     * @param realm
     *            the realm instance
     * @param s
     *            the string
     * @param lenient
     *            the lenient flag
     * @return the date in milli-seconds or {@code NaN} if not parsed successfully
     */
    public static double parseISOString(Realm realm, CharSequence s, boolean lenient) {
        // use a simple state machine to parse the input string
        final int ERROR = -1;
        final int YEAR = 0, MONTH = 1, DAY = 2;
        final int HOUR = 3, MIN = 4, SEC = 5, MSEC = 6;
        final int TZHOUR = 7, TZMIN = 8;
        int state = YEAR;
        // default values per [20.3.1.15 Date Time String Format]
        int[] values = { 1970, 1, 1, 0, 0, 0, 0, -1, -1 };
        int yearlen = 4, yearmod = 1, tzmod = 1;
        int i = 0, len = s.length();
        if (len != 0) {
            char c = s.charAt(0);
            if (c == '+' || c == '-') {
                // 20.3.1.15.1 Extended years
                i += 1;
                yearlen = 6;
                yearmod = (c == '-') ? -1 : 1;
            } else if (c == 'T') {
                // time-only forms no longer in spec, but follow spidermonkey here
                i += 1;
                state = HOUR;
            }
        }
        loop: while (state != ERROR) {
            if (state != MSEC) {
                int m = i + (state == YEAR ? yearlen : 2);
                if (m > len) {
                    state = ERROR;
                    break;
                }

                int value = 0;
                for (; i < m; ++i) {
                    char c = s.charAt(i);
                    if (c < '0' || c > '9') {
                        state = ERROR;
                        break loop;
                    }
                    value = 10 * value + (c - '0');
                }
                values[state] = value;
            } else {
                // common extension: 1..n milliseconds
                if (i >= len) {
                    state = ERROR;
                    break;
                }

                // no common behaviour: truncate or round?
                double value = 0, f = 0.1;
                for (int start = i; i < len; ++i, f *= 0.1) {
                    char c = s.charAt(i);
                    if (c < '0' || c > '9') {
                        if (i == start) {
                            state = ERROR;
                            break loop;
                        }
                        break;
                    }
                    value += f * (c - '0');
                }
                values[state] = (int) (1000 * value);
            }

            if (i == len) {
                // reached EOF, check for end state
                switch (state) {
                case HOUR:
                case TZHOUR:
                    state = ERROR;
                }
                break;
            }

            char c = s.charAt(i++);
            if (c == 'Z') {
                // handle abbrevation for UTC timezone
                values[TZHOUR] = 0;
                values[TZMIN] = 0;
                switch (state) {
                case MIN:
                case SEC:
                case MSEC:
                    break;
                default:
                    state = ERROR;
                }
                break;
            }

            // state transition
            switch (state) {
            case YEAR:
            case MONTH:
                state = (c == '-' ? state + 1 : c == 'T' ? HOUR : ERROR);
                break;
            case DAY:
                // allow ' ' as time separator in lenient mode
                state = (c == 'T' ? HOUR : lenient && c == ' ' ? HOUR : ERROR);
                break;
            case HOUR:
                state = (c == ':' ? MIN : ERROR);
                break;
            case TZHOUR:
                // state = (c == ':' ? state + 1 : ERROR);
                // Non-standard extension, https://bugzilla.mozilla.org/show_bug.cgi?id=682754
                if (c != ':') {
                    // back off by one and try to read without ':' separator
                    i -= 1;
                }
                state = TZMIN;
                break;
            case MIN:
                state = (c == ':' ? SEC : c == '+' || c == '-' ? TZHOUR : ERROR);
                break;
            case SEC:
                state = (c == '.' ? MSEC : c == '+' || c == '-' ? TZHOUR : ERROR);
                break;
            case MSEC:
                state = (c == '+' || c == '-' ? TZHOUR : ERROR);
                break;
            case TZMIN:
                state = ERROR;
                break;
            }
            if (state == TZHOUR) {
                // save timezone modificator
                tzmod = (c == '-') ? -1 : 1;
            }
        }

        syntax: {
            // error or unparsed characters
            if (state == ERROR || i != len)
                break syntax;

            // check values
            int year = values[YEAR], month = values[MONTH], day = values[DAY];
            int hour = values[HOUR], min = values[MIN], sec = values[SEC], msec = values[MSEC];
            int tzhour = values[TZHOUR], tzmin = values[TZMIN];
            if (year > 275943 // ceil(1e8/365) + 1970 = 275943
                    || (month < 1 || month > 12)
                    || (day < 1 || day > DaysInMonth(year, month))
                    || hour > 24 || (hour == 24 && (min > 0 || sec > 0 || msec > 0))
                    || min > 59
                    || sec > 59 || tzhour > 23 || tzmin > 59) {
                break syntax;
            }
            // valid ISO-8601 format, compute date in milliseconds
            double date = MakeDate(MakeDay(year * yearmod, month - 1, day),
                    MakeTime(hour, min, sec, msec));
            if (tzhour == -1) {
                // if time zone offset absent, interpret date-time as a local time
                date = UTC(realm, date);
            } else {
                date -= (tzhour * 60 + tzmin) * msPerMinute * tzmod;
            }

            if (date < -8.64e15 || date > 8.64e15)
                break syntax;
            return date;
        }

        // invalid ISO-8601 format, return NaN
        return Double.NaN;
    }

    private static final String[] weekDayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

    /**
     * Week Day Name
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the week day name
     */
    public static String WeekDayName(double t) {
        double weekDay = WeekDay(t);
        if (Double.isNaN(weekDay)) {
            return "";
        }
        return weekDayNames[(int) weekDay];
    }

    private static final String[] monthNames = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
            "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * Month Name
     * 
     * @param t
     *            the date in milli-seconds since the epoch
     * @return the month name
     */
    public static String MonthNameFromTime(double t) {
        double month = MonthFromTime(t);
        if (Double.isNaN(month)) {
            return "";
        }
        return monthNames[(int) month];
    }

    private static final Pattern utcDateTimePattern;
    private static final Pattern dateTimePattern;
    private static final Pattern usDateTimePattern;
    static {
        String weekday = "(?:([a-zA-Z]{3}),? )?";
        String time = "(?: ([0-2]?[0-9]):([0-5][0-9])(?::([0-5][0-9]))?(?: (AM|PM))?)?";
        String timezone;
        {
            String tzHour = "([+-][0-9]{2})";
            String tzMin = "([0-9]{2})";
            String tzOffset = "(?: ?" + tzHour + ":?" + tzMin + ")?";
            String tzName = "(?: \\([a-zA-Z]{3,5}\\))?";
            timezone = "( (?:GMT|UTC)?" + tzOffset + tzName + ")?";
        }

        // "EEE, dd MMM yyyy HH:mm:ss 'AM|PM' 'GMT'Z (z)"
        String utcDate = "([0-3]?[0-9]) ([a-zA-Z]{3}) (-?[0-9]{1,6})";
        utcDateTimePattern = Pattern.compile(weekday + utcDate + time + timezone);

        // "EEE, MMM dd yyyy HH:mm:ss 'AM|PM' 'GMT'Z (z)"
        String date = "([a-zA-Z]{3}) ([0-3]?[0-9]) (-?[0-9]{1,6})";
        dateTimePattern = Pattern.compile(weekday + date + time + timezone);

        // "mm/dd/yyyy HH:mm:ss 'AM|PM'"
        String usDate = "([0-1]?[0-9])/([0-3]?[0-9])/([0-9]{1,6})";
        usDateTimePattern = Pattern.compile(usDate + time);
    }

    /**
     * Parses a date-time string in "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)" format, returns
     * {@link Double#NaN} on mismatch.
     * 
     * @param realm
     *            the realm instance
     * @param s
     *            the string
     * @return the date in milli-seconds or {@code NaN} if not parsed successfully
     */
    public static double parseDateString(Realm realm, CharSequence s) {
        Matcher matcher;
        if ((matcher = utcDateTimePattern.matcher(s)).matches()) {
            assert matcher.groupCount() == 11;
            double day = fromDateString(matcher.group(4), matcher.group(3), matcher.group(2),
                    matcher.group(1));
            double time = fromTimeString(matcher.group(5), matcher.group(6), matcher.group(7),
                    matcher.group(8));
            double tzOffset = 0;
            boolean localTime = matcher.group(9) == null;
            if (!localTime) {
                tzOffset = fromTimeZoneString(matcher.group(10), matcher.group(11));
            }
            return fromDateTimeString(realm, day, time, tzOffset, localTime);
        }
        if ((matcher = dateTimePattern.matcher(s)).matches()) {
            assert matcher.groupCount() == 11;
            double day = fromDateString(matcher.group(4), matcher.group(2), matcher.group(3),
                    matcher.group(1));
            double time = fromTimeString(matcher.group(5), matcher.group(6), matcher.group(7),
                    matcher.group(8));
            double tzOffset = 0;
            boolean localTime = matcher.group(9) == null;
            if (!localTime) {
                tzOffset = fromTimeZoneString(matcher.group(10), matcher.group(11));
            }
            return fromDateTimeString(realm, day, time, tzOffset, localTime);
        }
        if ((matcher = usDateTimePattern.matcher(s)).matches()) {
            assert matcher.groupCount() == 7;
            double day = fromDateString(matcher.group(3), matcher.group(1), matcher.group(2));
            double time = fromTimeString(matcher.group(4), matcher.group(5), matcher.group(6),
                    matcher.group(7));
            double tzOffset = 0;
            boolean localTime = true;
            return fromDateTimeString(realm, day, time, tzOffset, localTime);
        }
        return Double.NaN;
    }

    private static double fromDateTimeString(Realm realm, double day, double time, double tzOffset,
            boolean localTime) {
        if (Double.isNaN(day) || Double.isNaN(time) || Double.isNaN(tzOffset))
            return Double.NaN;
        double date = MakeDate(day, time);
        if (localTime) {
            date = UTC(realm, date);
        } else {
            date -= tzOffset;
        }
        if (date < -8.64e15 || date > 8.64e15)
            return Double.NaN;
        return date;
    }

    private static double fromDateString(String yearValue, String monthName, String dayValue,
            String weekdayName) {
        assert yearValue != null && monthName != null && dayValue != null;
        int month = 1 + indexOf(monthNames, monthName);
        if (weekdayName != null) {
            // Just parse, but ignore actual value.
            int weekday = indexOf(weekDayNames, weekdayName);
            if (weekday == -1) {
                return Double.NaN;
            }
        }
        int year = Integer.parseInt(yearValue);
        int day = Integer.parseInt(dayValue);
        return fromDateString(year, month, day);
    }

    private static double fromDateString(String yearValue, String monthValue, String dayValue) {
        assert yearValue != null && monthValue != null && dayValue != null;
        int year = Integer.parseInt(yearValue);
        int month = Integer.parseInt(monthValue);
        int day = Integer.parseInt(dayValue);
        return fromDateString(year, month, day);
    }

    private static double fromDateString(int year, int month, int day) {
        if (Math.abs(year) > 275943 // ceil(1e8/365) + 1970 = 275943
                || (month < 1 || month > 12) || (day < 1 || day > DaysInMonth(year, month))) {
            return Double.NaN;
        }
        return MakeDay(year, month - 1, day);
    }

    private static double fromTimeString(String hourValue, String minValue, String secValue,
            String amPm) {
        if (hourValue == null) {
            assert minValue == null && secValue == null && amPm == null;
            return MakeTime(0, 0, 0, 0);
        }
        assert minValue != null;
        int hour = Integer.parseInt(hourValue);
        int min = Integer.parseInt(minValue);
        int sec = secValue != null ? Integer.parseInt(secValue) : 0;
        if (amPm == null) {
            if (hour > 24 || (hour == 24 && (min > 0 || sec > 0)) || min > 59 || sec > 59) {
                return Double.NaN;
            }
        } else {
            if (hour > 12 || min > 59 || sec > 59) {
                return Double.NaN;
            }
            if (hour == 12) {
                if ("AM".equals(amPm)) {
                    hour = 0;
                }
            } else {
                if ("PM".equals(amPm)) {
                    hour += 12;
                }
            }
        }
        return MakeTime(hour, min, sec, 0);
    }

    private static double fromTimeZoneString(String tzHourValue, String tzMinValue) {
        if (tzHourValue == null) {
            assert tzMinValue == null;
            return 0;
        }
        assert tzMinValue != null;
        int tzhour = Integer.parseInt(tzHourValue);
        int tzmin = Integer.parseInt(tzMinValue);
        if (Math.abs(tzhour) > 23 || Math.abs(tzmin) > 59) {
            return Double.NaN;
        }
        if (tzhour < 0) {
            return (tzhour * 60 - tzmin) * msPerMinute;
        }
        return (tzhour * 60 + tzmin) * msPerMinute;
    }

    private static int indexOf(String[] array, String value) {
        for (int i = 0, len = array.length; i < len; ++i) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
}
