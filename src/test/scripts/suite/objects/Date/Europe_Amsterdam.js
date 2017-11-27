/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
System.load("lib/assert-datetime.jsm");
System.load("lib/datetime.jsm");

const {
  assertDate
} = System.get("lib/assert-datetime.jsm");
const {
  DateTime, DayOfWeek, Month, TimeZone,
  LOCAL_TIME,
} = System.get("lib/datetime.jsm");

// Europe/Amsterdam as an example for mean time like timezones after LMT (AMT, NST).

setTimeZone("Europe/Amsterdam");

{
  let local = new DateTime.Local(1935, Month.January, 1, DayOfWeek.Tuesday, 0, 0, 0);
  let utc = LOCAL_TIME
            ? new DateTime.UTC(1934, Month.December, 31, DayOfWeek.Monday, 23, 40, 28)
            : new DateTime.UTC(1934, Month.December, 31, DayOfWeek.Monday, 23, 40, 0);

  LOCAL_TIME
  ? assertDate(local, utc, TimeZone(+0,19,32), {
      String: "Tue Jan 01 1935 00:00:00 GMT+0019 (AMT)",
      UTCString: "Mon, 31 Dec 1934 23:40:28 GMT",
    })
  : assertDate(local, utc, TimeZone(+0,20), {
      String: "Tue Jan 01 1935 00:00:00 GMT+0020 (+0020)",
      UTCString: "Mon, 31 Dec 1934 23:40:00 GMT",
    });
}

{
  let local = new DateTime.Local(1935, Month.July, 1, DayOfWeek.Monday, 0, 0, 0);
  let utc = LOCAL_TIME
            ? new DateTime.UTC(1935, Month.June, 30, DayOfWeek.Sunday, 22, 40, 28)
            : new DateTime.UTC(1935, Month.June, 30, DayOfWeek.Sunday, 22, 40, 0);

  LOCAL_TIME
  ? assertDate(local, utc, TimeZone(+1,19,32), {
      String: "Mon Jul 01 1935 00:00:00 GMT+0119 (NST)",
      UTCString: "Sun, 30 Jun 1935 22:40:28 GMT",
    })
  : assertDate(local, utc, TimeZone(+1,20), {
      String: "Mon Jul 01 1935 00:00:00 GMT+0120 (+0120)",
      UTCString: "Sun, 30 Jun 1935 22:40:00 GMT",
    });
}
