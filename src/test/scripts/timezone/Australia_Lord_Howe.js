/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Australia/Lord_Howe time zone offset is +10:30 and daylight savings amount is 00:30.

{
  // +10:30 (standard time)
  let local = new DateTime.Local(2010, Month.August, 1, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.July, 31, DayOfWeek.Saturday, 13, 30, 0);

  assertDate(local, utc, TimeZone(+10,30), {
    String: "Sun Aug 01 2010 00:00:00 GMT+1030 (LHST)",
    DateString: "Sun Aug 01 2010",
    TimeString: "00:00:00 GMT+1030 (LHST)",
    UTCString: "Sat, 31 Jul 2010 13:30:00 GMT",
    ISOString: "2010-07-31T13:30:00.000Z",
    LocaleString: "Sun, 08/01/2010, 12:00:00 AM GMT+10:30",
    LocaleDateString: "Sun, 08/01/2010",
    LocaleTimeString: "12:00:00 AM GMT+10:30",
  });
}

{
  // +10:30 (daylight savings)
  let local = new DateTime.Local(2010, Month.January, 3, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.January, 2, DayOfWeek.Saturday, 13, 0, 0);

  assertDate(local, utc, TimeZone(+11), {
    String: "Sun Jan 03 2010 00:00:00 GMT+1100 (LHDT)",
    DateString: "Sun Jan 03 2010",
    TimeString: "00:00:00 GMT+1100 (LHDT)",
    UTCString: "Sat, 02 Jan 2010 13:00:00 GMT",
    ISOString: "2010-01-02T13:00:00.000Z",
    LocaleString: "Sun, 01/03/2010, 12:00:00 AM GMT+11",
    LocaleDateString: "Sun, 01/03/2010",
    LocaleTimeString: "12:00:00 AM GMT+11",
  });
}
