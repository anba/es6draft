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
  DateTime, DayOfWeek, Month, TimeZone
} = System.get("lib/datetime.jsm");

// Pacific/Kiritimati time zone offset is +14:00.

setTimeZone("Pacific/Kiritimati");

{
  // +14:00 (standard time)
  let local = new DateTime.Local(2010, Month.August, 1, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.July, 31, DayOfWeek.Saturday, 10, 0, 0);

  assertDate(local, utc, TimeZone(+14), {
    String: "Sun Aug 01 2010 00:00:00 GMT+1400 (+14)",
    DateString: "Sun Aug 01 2010",
    TimeString: "00:00:00 GMT+1400 (+14)",
    UTCString: "Sat, 31 Jul 2010 10:00:00 GMT",
    ISOString: "2010-07-31T10:00:00.000Z",
    LocaleString: "Sun, 08/01/2010, 12:00:00 AM GMT+14",
    LocaleDateString: "Sun, 08/01/2010",
    LocaleTimeString: "12:00:00 AM GMT+14",
  });
}

// Pacific/Kiritimati time zone offset was -10:40 until Oct. 1979.

{
  // -10:40 (standard time)
  let local = new DateTime.Local(1975, Month.January, 1, DayOfWeek.Wednesday, 0, 0, 0);
  let utc = new DateTime.UTC(1975, Month.January, 1, DayOfWeek.Wednesday, 10, 40, 0);

  assertDate(local, utc, TimeZone(-10,40), {
    String: "Wed Jan 01 1975 00:00:00 GMT-1040 (-1040)",
    DateString: "Wed Jan 01 1975",
    TimeString: "00:00:00 GMT-1040 (-1040)",
    UTCString: "Wed, 01 Jan 1975 10:40:00 GMT",
    ISOString: "1975-01-01T10:40:00.000Z",
    LocaleString: "Wed, 01/01/1975, 12:00:00 AM GMT-10:40",
    LocaleDateString: "Wed, 01/01/1975",
    LocaleTimeString: "12:00:00 AM GMT-10:40",
  });
}
