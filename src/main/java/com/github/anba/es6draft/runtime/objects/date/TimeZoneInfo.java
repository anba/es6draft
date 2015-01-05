/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.date;

import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;

import com.ibm.icu.text.TimeZoneNames;
import com.ibm.icu.util.ULocale;

/**
 * Historic timezone names are not supported in Java.
 * 
 * @see https://bugs.openjdk.java.net/browse/JDK-4255109
 */
abstract class TimeZoneInfo {
    private static final long TODAY = System.currentTimeMillis();
    private static final TimeZoneInfo INSTANCE = new JodaTimeZoneInfo();

    /**
     * Returns the default {@link TimeZoneInfo} instance.
     * 
     * @return the default instance
     */
    public static final TimeZoneInfo getDefault() {
        return INSTANCE;
    }

    /**
     * Returns the local time.
     * 
     * @param tz
     *            the timezone
     * @param date
     *            the date in milli-seconds since the start of the epoch
     * @return the date in local time
     */
    public abstract long localTime(TimeZone tz, long date);

    /**
     * Returns the UTC time.
     * 
     * @param tz
     *            the timezone
     * @param localTime
     *            the date in local time
     * @return the date in milli-seconds since the start of the epoch
     */
    public abstract long utc(TimeZone tz, long localTime);

    /**
     * Returns {@code true} if the specified date is in daylight savings time.
     * 
     * @param tz
     *            the timezone
     * @param date
     *            the date in milli-seconds since the start of the epoch
     * @return {@code true} if the date is in DST
     * 
     * @see TimeZone#inDaylightTime(Date)
     */
    public abstract boolean inDaylightTime(TimeZone tz, long date);

    /**
     * Returns the amount of saved daylight savings time, typically one hour. If the given date is
     * not in DST, this method returns {@code 0}.
     * 
     * @param tz
     *            the timezone
     * @param date
     *            the date in milli-seconds since the start of the epoch
     * @return the amount of saved daylight savings time
     * 
     * @see TimeZone#getDSTSavings()
     */
    public abstract int getDSTSavings(TimeZone tz, long date);

    /**
     * Returns the offset from UTC in milli-seconds, including DST savings.
     * 
     * @param tz
     *            the timezone
     * @param date
     *            the date in milli-seconds since the start of the epoch
     * @return the local offset adjusted with DST
     * 
     * @see TimeZone#getOffset(long)
     */
    public abstract int getOffset(TimeZone tz, long date);

    /**
     * Returns the current offset from UTC in milli-seconds.
     * 
     * @param tz
     *            the timezone
     * @return the current local offset
     * 
     * @see TimeZone#getRawOffset()
     */
    public abstract int getRawOffset(TimeZone tz);

    /**
     * Returns the offset from UTC in milli-seconds.
     * 
     * @param tz
     *            the timezone
     * @param date
     *            the date in milli-seconds since the start of the epoch
     * @return the local offset
     * 
     * @see TimeZone#getRawOffset()
     */
    public abstract int getRawOffset(TimeZone tz, long date);

    /**
     * Returns the display name for the given time zone and date.
     * 
     * @param tz
     *            the timezone
     * @param date
     *            the date in milli-seconds since the start of the epoch
     * @return the display name
     * 
     * @see TimeZone#getDisplayName()
     */
    public abstract String getDisplayName(TimeZone tz, long date);

    private static final class TimeZoneRef<T> extends SoftReference<T> {
        private final String id;

        public TimeZoneRef(String id, T referent) {
            super(referent);
            this.id = id;
        }

        public T get(String id) {
            if (id.equals(this.id)) {
                return get();
            }
            return null;
        }
    }

    static final class JDKTimeZoneInfo extends TimeZoneInfo {
        @Override
        public long localTime(TimeZone tz, long date) {
            return date + tz.getOffset(date);
        }

        @Override
        public long utc(TimeZone tz, long localTime) {
            long date = localTime - tz.getRawOffset();
            return localTime - tz.getOffset(date);
        }

        @Override
        public boolean inDaylightTime(TimeZone tz, long date) {
            return tz.inDaylightTime(new Date(date));
        }

        @Override
        public int getDSTSavings(TimeZone tz, long date) {
            if (tz.inDaylightTime(new Date(date))) {
                return tz.getDSTSavings();
            }
            return 0;
        }

        @Override
        public int getOffset(TimeZone tz, long date) {
            return tz.getOffset(date);
        }

        @Override
        public int getRawOffset(TimeZone tz) {
            return getRawOffset(tz, TODAY);
        }

        @Override
        public int getRawOffset(TimeZone tz, long date) {
            return tz.getRawOffset();
        }

        @Override
        public String getDisplayName(TimeZone tz, long date) {
            boolean daylightSavings = tz.inDaylightTime(new Date(date));
            if (!daylightSavings && tz.getOffset(date) != tz.getRawOffset()) {
                daylightSavings = tz.useDaylightTime();
            }
            return tz.getDisplayName(daylightSavings, TimeZone.SHORT, Locale.US);
        }
    }

    static final class ICUTimeZoneInfo extends TimeZoneInfo {
        private static TimeZoneRef<com.ibm.icu.util.TimeZone> lastTimeZone = new TimeZoneRef<>(
                null, null);

        private static com.ibm.icu.util.TimeZone toICUTimeZone(TimeZone tz) {
            TimeZoneRef<com.ibm.icu.util.TimeZone> ref = lastTimeZone;
            String id = tz.getID();
            com.ibm.icu.util.TimeZone timeZone = ref.get(id);
            if (timeZone != null) {
                return timeZone;
            }
            timeZone = com.ibm.icu.util.TimeZone.getTimeZone(id).freeze();
            lastTimeZone = new TimeZoneRef<>(id, timeZone);
            return timeZone;
        }

        @Override
        public long localTime(TimeZone tz, long date) {
            return date + toICUTimeZone(tz).getOffset(date);
        }

        @Override
        public long utc(TimeZone tz, long localTime) {
            com.ibm.icu.util.TimeZone timeZone = toICUTimeZone(tz);
            long date = localTime - timeZone.getRawOffset();
            return localTime - timeZone.getOffset(date);
        }

        @Override
        public boolean inDaylightTime(TimeZone tz, long date) {
            return toICUTimeZone(tz).inDaylightTime(new Date(date));
        }

        @Override
        public int getDSTSavings(TimeZone tz, long date) {
            int[] offsets = new int[2];
            toICUTimeZone(tz).getOffset(date, false, offsets);
            return offsets[1];
        }

        @Override
        public int getOffset(TimeZone tz, long date) {
            return toICUTimeZone(tz).getOffset(date);
        }

        @Override
        public int getRawOffset(TimeZone tz) {
            return toICUTimeZone(tz).getRawOffset();
        }

        @Override
        public int getRawOffset(TimeZone tz, long date) {
            int[] offsets = new int[2];
            toICUTimeZone(tz).getOffset(date, false, offsets);
            return offsets[0];
        }

        @Override
        public String getDisplayName(TimeZone tz, long date) {
            com.ibm.icu.util.TimeZone timeZone = toICUTimeZone(tz);
            int[] offsets = new int[2];
            timeZone.getOffset(date, false, offsets);
            boolean daylightSavings = offsets[1] != 0;
            if (!daylightSavings && offsets[0] != timeZone.getRawOffset()) {
                daylightSavings = timeZone.useDaylightTime();
            }
            TimeZoneNames.NameType nameType = daylightSavings ? TimeZoneNames.NameType.SHORT_DAYLIGHT
                    : TimeZoneNames.NameType.SHORT_STANDARD;
            TimeZoneNames tzNames = TimeZoneNames.getTZDBInstance(ULocale.US);
            String displayName = tzNames.getDisplayName(timeZone.getID(), nameType, date);
            if (displayName != null) {
                return displayName;
            }
            return tz.getDisplayName(daylightSavings, TimeZone.SHORT, Locale.US);
        }
    }

    static final class JodaTimeZoneInfo extends TimeZoneInfo {
        // Last transition from LMT (Asia/Aden on 01 January, 1950).
        private static final long LAST_LMT_TRANSITION = -631162794000L;
        private static final long LOCAL_LAST_LMT_TRANSITION = -631152000000L;
        private static final boolean IGNORE_LMT = false;

        // Last transition from local time (Africa/Monrovia on 01 May, 1972).
        private static final long LAST_LOCAL_TRANSITION = 73529070000L;
        private static final long LOCAL_LAST_LOCAL_TRANSITION = 73526400000L;
        private static final boolean IGNORE_LOCAL = false;

        // Start of the epoch (01 January, 1970).
        private static final long EPOCH = 0L;
        private static final boolean IGNORE_LOCAL_BEFORE_EPOCH = true;

        static {
            assert !((IGNORE_LMT | IGNORE_LOCAL) & (IGNORE_LMT | IGNORE_LOCAL_BEFORE_EPOCH) & (IGNORE_LOCAL | IGNORE_LOCAL_BEFORE_EPOCH));
        }

        private static TimeZoneRef<DateTimeZone> lastTimeZone = new TimeZoneRef<>(null, null);

        private static DateTimeZone toDateTimeZone(TimeZone tz) {
            TimeZoneRef<DateTimeZone> ref = lastTimeZone;
            String id = tz.getID();
            DateTimeZone timeZone = ref.get(id);
            if (timeZone != null) {
                return timeZone;
            }
            timeZone = DateTimeZone.forID(id);
            lastTimeZone = new TimeZoneRef<>(id, timeZone);
            return timeZone;
        }

        private static long toFirstDateAfterLocalMeanTime(DateTimeZone timeZone, long date) {
            assert date <= LAST_LMT_TRANSITION;
            while ("LMT".equals(timeZone.getNameKey(date))) {
                date = timeZone.nextTransition(date);
            }
            return date;
        }

        private static boolean isNormalizedOffset(int offset) {
            offset /= 1000;
            // Multiple of 15 or 20 minutes.
            return (offset % (15 * 60) == 0) || (offset % (20 * 60) == 0);
        }

        private static long toFirstDateWithNormalizedTime(DateTimeZone timeZone, long date) {
            assert date <= LAST_LOCAL_TRANSITION;
            long normalized = date;
            while (!isNormalizedOffset(timeZone.getOffset(normalized))) {
                normalized = timeZone.nextTransition(normalized);
            }
            if (normalized != date
                    && timeZone.isStandardOffset(date) != timeZone.isStandardOffset(normalized)) {
                long next = timeZone.nextTransition(normalized);
                if (timeZone.isStandardOffset(date) == timeZone.isStandardOffset(next)) {
                    normalized = next;
                }
            }
            return normalized;
        }

        @Override
        public long localTime(TimeZone tz, long date) {
            DateTimeZone timeZone = toDateTimeZone(tz);
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (date < EPOCH) {
                    return date + getOffset(timeZone, date);
                }
            } else if (IGNORE_LOCAL) {
                if (date <= LAST_LOCAL_TRANSITION) {
                    return date + getOffset(timeZone, date);
                }
            } else if (IGNORE_LMT) {
                if (date <= LAST_LMT_TRANSITION) {
                    return date + getOffset(timeZone, date);
                }
            }
            return timeZone.convertUTCToLocal(date);
        }

        @Override
        public long utc(TimeZone tz, long localTime) {
            DateTimeZone timeZone = toDateTimeZone(tz);
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (localTime < EPOCH) {
                    int offsetLocal = getOffset(timeZone, localTime);
                    int offset = getOffset(timeZone, localTime - offsetLocal);
                    return localTime - offset;
                }
            } else if (IGNORE_LOCAL) {
                if (localTime <= LOCAL_LAST_LOCAL_TRANSITION) {
                    int offsetLocal = getOffset(timeZone, localTime);
                    int offset = getOffset(timeZone, localTime - offsetLocal);
                    return localTime - offset;
                }
            } else if (IGNORE_LMT) {
                if (localTime <= LOCAL_LAST_LMT_TRANSITION) {
                    int offsetLocal = getOffset(timeZone, localTime);
                    int offset = getOffset(timeZone, localTime - offsetLocal);
                    return localTime - offset;
                }
            }
            return timeZone.convertLocalToUTC(localTime, false);
        }

        @Override
        public boolean inDaylightTime(TimeZone tz, long date) {
            DateTimeZone timeZone = toDateTimeZone(tz);
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (date < EPOCH) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LOCAL) {
                if (date <= LAST_LOCAL_TRANSITION) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LMT) {
                if (date <= LAST_LMT_TRANSITION) {
                    date = toFirstDateAfterLocalMeanTime(timeZone, date);
                }
            }
            return !timeZone.isStandardOffset(date);
        }

        @Override
        public int getDSTSavings(TimeZone tz, long date) {
            DateTimeZone timeZone = toDateTimeZone(tz);
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (date < EPOCH) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LOCAL) {
                if (date <= LAST_LOCAL_TRANSITION) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LMT) {
                if (date <= LAST_LMT_TRANSITION) {
                    date = toFirstDateAfterLocalMeanTime(timeZone, date);
                }
            }
            return Math.abs(timeZone.getOffset(date) - timeZone.getStandardOffset(date));
        }

        @Override
        public int getOffset(TimeZone tz, long date) {
            return getOffset(toDateTimeZone(tz), date);
        }

        private int getOffset(DateTimeZone timeZone, long date) {
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (date < EPOCH) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LOCAL) {
                if (date <= LAST_LOCAL_TRANSITION) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LMT) {
                if (date <= LAST_LMT_TRANSITION) {
                    date = toFirstDateAfterLocalMeanTime(timeZone, date);
                }
            }
            return timeZone.getOffset(date);
        }

        @Override
        public int getRawOffset(TimeZone tz) {
            return toDateTimeZone(tz).getStandardOffset(TODAY);
        }

        @Override
        public int getRawOffset(TimeZone tz, long date) {
            DateTimeZone timeZone = toDateTimeZone(tz);
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (date < EPOCH) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LOCAL) {
                if (date <= LAST_LOCAL_TRANSITION) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LMT) {
                if (date <= LAST_LMT_TRANSITION) {
                    date = toFirstDateAfterLocalMeanTime(timeZone, date);
                }
            }
            return timeZone.getStandardOffset(date);
        }

        @Override
        public String getDisplayName(TimeZone tz, long date) {
            DateTimeZone timeZone = toDateTimeZone(tz);
            if (IGNORE_LOCAL_BEFORE_EPOCH) {
                if (date < EPOCH) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LOCAL) {
                if (date <= LAST_LOCAL_TRANSITION) {
                    date = toFirstDateWithNormalizedTime(timeZone, date);
                }
            } else if (IGNORE_LMT) {
                if (date <= LAST_LMT_TRANSITION) {
                    date = toFirstDateAfterLocalMeanTime(timeZone, date);
                }
            }
            String displayName = timeZone.getNameKey(date);
            if (displayName != null) {
                return displayName;
            }
            boolean daylightSavings = !timeZone.isStandardOffset(date);
            if (!daylightSavings && timeZone.getOffset(date) != timeZone.getStandardOffset(TODAY)) {
                daylightSavings = timeZone.nextTransition(date) != date;
            }
            return tz.getDisplayName(daylightSavings, TimeZone.SHORT, Locale.US);
        }
    }
}
