/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Pacific/Chatham time zone offset is 12:45.

{
  // +12:45 (standard time)
  let local = new DateTime.Local(2010, Month.August, 1, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.July, 31, DayOfWeek.Saturday, 11, 15, 0);

  assertDate(local, utc, TimeZone(+12,45), {
    String: "Sun Aug 01 2010 00:00:00 GMT+1245 (CHAST)",
    DateString: "Sun Aug 01 2010",
    TimeString: "00:00:00 GMT+1245 (CHAST)",
    UTCString: "Sat, 31 Jul 2010 11:15:00 GMT",
    ISOString: "2010-07-31T11:15:00.000Z",
    LocaleString: "Sun, 08/01/2010, 12:00:00 AM GMT+12:45",
    LocaleDateString: "Sun, 08/01/2010",
    LocaleTimeString: "12:00:00 AM GMT+12:45",
  });
}

{
  // +12:45 (daylight savings)
  let local = new DateTime.Local(2010, Month.January, 3, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.January, 2, DayOfWeek.Saturday, 10, 15, 0);

  assertDate(local, utc, TimeZone(+13,45), {
    String: "Sun Jan 03 2010 00:00:00 GMT+1345 (CHADT)",
    DateString: "Sun Jan 03 2010",
    TimeString: "00:00:00 GMT+1345 (CHADT)",
    UTCString: "Sat, 02 Jan 2010 10:15:00 GMT",
    ISOString: "2010-01-02T10:15:00.000Z",
    LocaleString: "Sun, 01/03/2010, 12:00:00 AM GMT+13:45",
    LocaleDateString: "Sun, 01/03/2010",
    LocaleTimeString: "12:00:00 AM GMT+13:45",
  });
}
