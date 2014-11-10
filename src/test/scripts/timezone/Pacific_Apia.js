/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Pacific/Apia switched from -11:00 to +13:00 on 2011 Dec 29 24:00.

{
  // -11:00 (daylight savings)
  let local = new DateTime.Local(2011, Month.December, 29, DayOfWeek.Thursday, 0, 0, 0);
  let utc = new DateTime.UTC(2011, Month.December, 29, DayOfWeek.Thursday, 10, 0, 0);

  assertDate(local, utc, TimeZone(-10), {
    String: "Thu Dec 29 2011 00:00:00 GMT-1000 (SDT)",
    DateString: "Thu Dec 29 2011",
    TimeString: "00:00:00 GMT-1000 (SDT)",
    UTCString: "Thu, 29 Dec 2011 10:00:00 GMT",
    ISOString: "2011-12-29T10:00:00.000Z",
    LocaleString: "Thu, 12/29/2011, 12:00:00 AM GMT-10",
    LocaleDateString: "Thu, 12/29/2011",
    LocaleTimeString: "12:00:00 AM GMT-10",
  });
}

{
  // +13:00 (daylight savings)
  let local = new DateTime.Local(2011, Month.December, 31, DayOfWeek.Saturday, 0, 0, 0);
  let utc = new DateTime.UTC(2011, Month.December, 30, DayOfWeek.Friday, 10, 0, 0);

  assertDate(local, utc, TimeZone(+14), {
    String: "Sat Dec 31 2011 00:00:00 GMT+1400 (WSDT)",
    DateString: "Sat Dec 31 2011",
    TimeString: "00:00:00 GMT+1400 (WSDT)",
    UTCString: "Fri, 30 Dec 2011 10:00:00 GMT",
    ISOString: "2011-12-30T10:00:00.000Z",
    LocaleString: "Sat, 12/31/2011, 12:00:00 AM GMT+14",
    LocaleDateString: "Sat, 12/31/2011",
    LocaleTimeString: "12:00:00 AM GMT+14",
  });
}

{
  // +13:00 (standard time)
  let local = new DateTime.Local(2012, Month.April, 2, DayOfWeek.Monday, 0, 0, 0);
  let utc = new DateTime.UTC(2012, Month.April, 1, DayOfWeek.Sunday, 11, 0, 0);

  assertDate(local, utc, TimeZone(+13), {
    String: "Mon Apr 02 2012 00:00:00 GMT+1300 (WSST)",
    DateString: "Mon Apr 02 2012",
    TimeString: "00:00:00 GMT+1300 (WSST)",
    UTCString: "Sun, 01 Apr 2012 11:00:00 GMT",
    ISOString: "2012-04-01T11:00:00.000Z",
    LocaleString: "Mon, 04/02/2012, 12:00:00 AM GMT+13",
    LocaleDateString: "Mon, 04/02/2012",
    LocaleTimeString: "12:00:00 AM GMT+13",
  });
}
