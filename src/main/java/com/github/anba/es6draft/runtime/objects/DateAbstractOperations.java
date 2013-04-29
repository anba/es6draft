/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;

import java.text.DateFormatSymbols;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.anba.es6draft.runtime.Realm;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.9 Date Objects</h2>
 * <ul>
 * <li>15.9.1 Overview of Date Objects and Definitions of Abstract Operations
 * </ul>
 */
final class DateAbstractOperations {
    private DateAbstractOperations() {
    }

    private static final double modulo(double dividend, double divisor) {
        double remainder = dividend % divisor;
        return (remainder >= 0 ? remainder : remainder + divisor);
    }

    /**
     * 15.9.1.2 Day Number and Time within Day
     */
    public static final double msPerDay = 86400000;

    /**
     * 15.9.1.2 Day Number and Time within Day
     */
    public static double Day(double t) {
        return Math.floor(t / msPerDay);
    }

    /**
     * 15.9.1.2 Day Number and Time within Day
     */
    public static double TimeWithinDay(double t) {
        return modulo(t, msPerDay);
    }

    /**
     * 15.9.1.3 Year Number
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
     * 15.9.1.3 Year Number
     */
    public static double DayFromYear(double y) {
        return 365 * (y - 1970) + Math.floor((y - 1969) / 4) - Math.floor((y - 1901) / 100)
                + Math.floor((y - 1601) / 400);
    }

    /**
     * 15.9.1.3 Year Number
     */
    public static double TimeFromYear(double y) {
        return msPerDay * DayFromYear(y);
    }

    /**
     * 15.9.1.3 Year Number
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
     * 15.9.1.3 Year Number
     */
    public static boolean InLeapYear(double t) {
        return DaysInYear(YearFromTime(t)) == 366;
    }

    /**
     * 15.9.1.4 Month Number
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
     * 15.9.1.4 Month Number
     */
    public static double DayWithinYear(double t) {
        return Day(t) - DayFromYear(YearFromTime(t));
    }

    /**
     * 15.9.1.5 Date Number
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
     * 15.9.1.6 Week Day
     */
    public static double WeekDay(double t) {
        return modulo(Day(t) + 4, 7);
    }

    /**
     * 15.9.1.7 Local Time Zone Adjustment
     */
    public static double LocalTZA(Realm realm) {
        return realm.getTimezone().getRawOffset();
    }

    /**
     * 15.9.1.8 Daylight Saving Time Adjustment
     */
    public static double DaylightSavingTA(Realm realm, double t) {
        TimeZone tz = realm.getTimezone();
        if (tz.inDaylightTime(new Date((long) t))) {
            return tz.getDSTSavings();
        }
        return 0;
    }

    /**
     * 15.9.1.9 Local Time
     */
    public static double LocalTime(Realm realm, double t) {
        return t + LocalTZA(realm) + DaylightSavingTA(realm, t);
    }

    /**
     * 15.9.1.9 Local Time
     */
    public static double UTC(Realm realm, double t) {
        return t - LocalTZA(realm) - DaylightSavingTA(realm, t - LocalTZA(realm));
    }

    /**
     * 15.9.1.10 Hours, Minutes, Second, and Milliseconds
     */
    public static final double //
            HoursPerDay = 24, //
            MinutesPerHour = 60, //
            SecondsPerMinute = 60, //
            msPerSecond = 1000, //
            msPerMinute = msPerSecond * SecondsPerMinute, //
            msPerHour = msPerMinute * MinutesPerHour;

    /**
     * 15.9.1.10 Hours, Minutes, Second, and Milliseconds
     */
    public static double HourFromTime(double t) {
        return modulo(Math.floor(t / msPerHour), HoursPerDay);
    }

    /**
     * 15.9.1.10 Hours, Minutes, Second, and Milliseconds
     */
    public static double MinFromTime(double t) {
        return modulo(Math.floor(t / msPerMinute), MinutesPerHour);
    }

    /**
     * 15.9.1.10 Hours, Minutes, Second, and Milliseconds
     */
    public static double SecFromTime(double t) {
        return modulo(Math.floor(t / msPerSecond), SecondsPerMinute);
    }

    /**
     * 15.9.1.10 Hours, Minutes, Second, and Milliseconds
     */
    public static double msFromTime(double t) {
        return modulo(t, msPerSecond);
    }

    private static boolean isFinite(double d) {
        return !(Double.isNaN(d) || Double.isInfinite(d));
    }

    /**
     * 15.9.1.11 MakeTime (hour, min, sec, ms)
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
     * 15.9.1.12 MakeDay (year, month, date)
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
     * 15.9.1.13 MakeDate (day, time)
     */
    public static double MakeDate(double day, double time) {
        if (!isFinite(day) || !isFinite(time)) {
            return Double.NaN;
        }
        return day * msPerDay + time;
    }

    /**
     * 15.9.1.14 TimeClip (time)
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
     * 15.9.1.15 Date Time String Format<br>
     * Parse input string according to simplified ISO-8601 Extended Format:
     * <ul>
     * <li><code>YYYY-MM-DD'T'HH:mm:ss.sss'Z'</code></li>
     * <li>or <code>YYYY-MM-DD'T'HH:mm:ss.sss[+-]hh:mm</code></li>
     * </ul>
     */
    public static double parseISOString(Realm realm, CharSequence s, boolean lenient) {
        // use a simple state machine to parse the input string
        final int ERROR = -1;
        final int YEAR = 0, MONTH = 1, DAY = 2;
        final int HOUR = 3, MIN = 4, SEC = 5, MSEC = 6;
        final int TZHOUR = 7, TZMIN = 8;
        int state = YEAR;
        // default values per [15.9.1.15 Date Time String Format]
        int[] values = { 1970, 1, 1, 0, 0, 0, 0, -1, -1 };
        int yearlen = 4, yearmod = 1, tzmod = 1;
        int i = 0, len = s.length();
        if (len != 0) {
            char c = s.charAt(0);
            if (c == '+' || c == '-') {
                // 15.9.1.15.1 Extended years
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
                // allow ' ' as time separator in lenient mode (to be able to parse our UTC strings)
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

    private static final Pattern dateTimePattern;
    static {
        // "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)"
        String date = "([a-zA-Z]{3}) ([a-zA-Z]{3}) ([0-3][0-9]) (-?[0-9]{1,6})";
        String time = "([0-2][0-9]):([0-5][0-9]):([0-5][0-9])";
        String timezone = "GMT([+-][0-9]{4})(?: \\([a-zA-Z]{3,5}\\))?";
        dateTimePattern = Pattern.compile(date + " " + time + " " + timezone);
    }

    /**
     * Parse a date-time string in "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)" format, returns
     * {@link Double#NaN} on mismatch
     */
    public static double parseDateString(Realm realm, CharSequence s) {
        Matcher matcher = dateTimePattern.matcher(s);
        syntax: if (matcher.matches()) {
            DateFormatSymbols symbols = DateFormatSymbols.getInstance(Locale.US);
            int weekday = indexOf(symbols.getShortWeekdays(), matcher.group(1), 1, 7);
            int month = 1 + indexOf(symbols.getShortMonths(), matcher.group(2), 0, 11);
            int day = Integer.parseInt(matcher.group(3));
            int year = Integer.parseInt(matcher.group(4));
            int hour = Integer.parseInt(matcher.group(5));
            int min = Integer.parseInt(matcher.group(6));
            int sec = Integer.parseInt(matcher.group(7));
            int msec = 0;
            int tz = Integer.parseInt(matcher.group(8));
            int tzhour = tz / 100;
            int tzmin = tz % 100;

            // just parse, but ignore actual value
            if (weekday == -1) {
                break syntax;
            }
            if (Math.abs(year) > 275943 // ceil(1e8/365) + 1970 = 275943
                    || (month < 1 || month > 12)
                    || (day < 1 || day > DaysInMonth(year, month))
                    || hour > 24 || (hour == 24 && (min > 0 || sec > 0 || msec > 0))
                    || min > 59
                    || sec > 59 || Math.abs(tzhour) > 23 || Math.abs(tzmin) > 59) {
                break syntax;
            }
            double date = MakeDate(MakeDay(year, month - 1, day), MakeTime(hour, min, sec, msec));
            date -= (tzhour * 60 + tzmin) * msPerMinute;

            if (date < -8.64e15 || date > 8.64e15)
                break syntax;
            return date;
        }
        return Double.NaN;
    }

    private static final int indexOf(String[] array, String value, int startIndex, int endIndex) {
        assert startIndex >= 0 && endIndex < array.length && startIndex <= endIndex;
        for (int i = startIndex; i <= endIndex; ++i) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }
}
