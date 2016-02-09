/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Pacific/Niue time zone offset was -11:20 until 1951.

{
  // -11:20 (standard time)
  let local = new DateTime.Local(1950, Month.January, 1, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(1950, Month.January, 1, DayOfWeek.Sunday, 11, 20, 0);

  assertDate(local, utc, TimeZone(-11,20), {
    String: "Sun Jan 01 1950 00:00:00 GMT-1120 (NUT)",
    DateString: "Sun Jan 01 1950",
    TimeString: "00:00:00 GMT-1120 (NUT)",
    UTCString: "Sun, 01 Jan 1950 11:20:00 GMT",
    ISOString: "1950-01-01T11:20:00.000Z",
    LocaleString: "Sun, 01/01/1950, 12:00:00 AM GMT-11:20",
    LocaleDateString: "Sun, 01/01/1950",
    LocaleTimeString: "12:00:00 AM GMT-11:20",
  });
}
