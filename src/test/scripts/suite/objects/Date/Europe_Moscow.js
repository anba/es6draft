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

setTimeZone("Europe/Moscow");

{
  let local = new DateTime.Local(1970, Month.January, 1, DayOfWeek.Thursday, 0, 0, 0);
  let utc = new DateTime.UTC(1969, Month.December, 31, DayOfWeek.Wednesday, 21, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Thu Jan 01 1970 00:00:00 GMT+0300 (MSK)",
    DateString: "Thu Jan 01 1970",
    TimeString: "00:00:00 GMT+0300 (MSK)",
    UTCString: "Wed, 31 Dec 1969 21:00:00 GMT",
    ISOString: "1969-12-31T21:00:00.000Z",
    LocaleString: "Thu, 01/01/1970, 12:00:00 AM GMT+3",
    LocaleDateString: "Thu, 01/01/1970",
    LocaleTimeString: "12:00:00 AM GMT+3",
  });
}

// Russia was in +02:00 starting on 1991-03-31 until 1992-01-19,
// while still observing DST (transitions 1991-03-31 and 1991-09-29).

{
  // +03:00 (daylight savings)
  let local = new DateTime.Local(1990, Month.September, 1, DayOfWeek.Saturday, 0, 0, 0);
  let utc = new DateTime.UTC(1990, Month.August, 31, DayOfWeek.Friday, 20, 0, 0);

  assertDate(local, utc, TimeZone(+4), {
    String: "Sat Sep 01 1990 00:00:00 GMT+0400 (MSD)",
    DateString: "Sat Sep 01 1990",
    TimeString: "00:00:00 GMT+0400 (MSD)",
    UTCString: "Fri, 31 Aug 1990 20:00:00 GMT",
    ISOString: "1990-08-31T20:00:00.000Z",
    LocaleString: "Sat, 09/01/1990, 12:00:00 AM GMT+4",
    LocaleDateString: "Sat, 09/01/1990",
    LocaleTimeString: "12:00:00 AM GMT+4",
  });
}

{
  // +03:00 (standard time)
  let local = new DateTime.Local(1991, Month.March, 25, DayOfWeek.Monday, 0, 0, 0);
  let utc = new DateTime.UTC(1991, Month.March, 24, DayOfWeek.Sunday, 21, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Mon Mar 25 1991 00:00:00 GMT+0300 (MSK)",
    DateString: "Mon Mar 25 1991",
    TimeString: "00:00:00 GMT+0300 (MSK)",
    UTCString: "Sun, 24 Mar 1991 21:00:00 GMT",
    ISOString: "1991-03-24T21:00:00.000Z",
    LocaleString: "Mon, 03/25/1991, 12:00:00 AM GMT+3",
    LocaleDateString: "Mon, 03/25/1991",
    LocaleTimeString: "12:00:00 AM GMT+3",
  });
}

{
  // +02:00 (daylight savings)
  let local = new DateTime.Local(1991, Month.March, 31, DayOfWeek.Sunday, 12, 0, 0);
  let utc = new DateTime.UTC(1991, Month.March, 31, DayOfWeek.Sunday, 9, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Sun Mar 31 1991 12:00:00 GMT+0300 (EEST)",
    DateString: "Sun Mar 31 1991",
    TimeString: "12:00:00 GMT+0300 (EEST)",
    UTCString: "Sun, 31 Mar 1991 09:00:00 GMT",
    ISOString: "1991-03-31T09:00:00.000Z",
    LocaleString: "Sun, 03/31/1991, 12:00:00 PM GMT+3",
    LocaleDateString: "Sun, 03/31/1991",
    LocaleTimeString: "12:00:00 PM GMT+3",
  });
}

{
  // +02:00 (daylight savings)
  let local = new DateTime.Local(1991, Month.September, 28, DayOfWeek.Saturday, 0, 0, 0);
  let utc = new DateTime.UTC(1991, Month.September, 27, DayOfWeek.Friday, 21, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Sat Sep 28 1991 00:00:00 GMT+0300 (EEST)",
    DateString: "Sat Sep 28 1991",
    TimeString: "00:00:00 GMT+0300 (EEST)",
    UTCString: "Fri, 27 Sep 1991 21:00:00 GMT",
    ISOString: "1991-09-27T21:00:00.000Z",
    LocaleString: "Sat, 09/28/1991, 12:00:00 AM GMT+3",
    LocaleDateString: "Sat, 09/28/1991",
    LocaleTimeString: "12:00:00 AM GMT+3",
  });
}

{
  // +02:00 (standard time)
  let local = new DateTime.Local(1991, Month.September, 30, DayOfWeek.Monday, 0, 0, 0);
  let utc = new DateTime.UTC(1991, Month.September, 29, DayOfWeek.Sunday, 22, 0, 0);

  assertDate(local, utc, TimeZone(+2), {
    String: "Mon Sep 30 1991 00:00:00 GMT+0200 (EET)",
    DateString: "Mon Sep 30 1991",
    TimeString: "00:00:00 GMT+0200 (EET)",
    UTCString: "Sun, 29 Sep 1991 22:00:00 GMT",
    ISOString: "1991-09-29T22:00:00.000Z",
    LocaleString: "Mon, 09/30/1991, 12:00:00 AM GMT+2",
    LocaleDateString: "Mon, 09/30/1991",
    LocaleTimeString: "12:00:00 AM GMT+2",
  });
}

// Russia stopped observing DST in Oct. 2010 (last transition on 2010-10-31),
// and changed timezone from +03:00 to +04:00 on 2011-03-27.

{
  // +03:00 (daylight savings)
  let local = new DateTime.Local(2010, Month.October, 30, DayOfWeek.Saturday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.October, 29, DayOfWeek.Friday, 20, 0, 0);

  assertDate(local, utc, TimeZone(+4), {
    String: "Sat Oct 30 2010 00:00:00 GMT+0400 (MSD)",
    DateString: "Sat Oct 30 2010",
    TimeString: "00:00:00 GMT+0400 (MSD)",
    UTCString: "Fri, 29 Oct 2010 20:00:00 GMT",
    ISOString: "2010-10-29T20:00:00.000Z",
    LocaleString: "Sat, 10/30/2010, 12:00:00 AM GMT+4",
    LocaleDateString: "Sat, 10/30/2010",
    LocaleTimeString: "12:00:00 AM GMT+4",
  });
}

{
  // +03:00 (standard time)
  let local = new DateTime.Local(2010, Month.November, 1, DayOfWeek.Monday, 0, 0, 0);
  let utc = new DateTime.UTC(2010, Month.October, 31, DayOfWeek.Sunday, 21, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Mon Nov 01 2010 00:00:00 GMT+0300 (MSK)",
    DateString: "Mon Nov 01 2010",
    TimeString: "00:00:00 GMT+0300 (MSK)",
    UTCString: "Sun, 31 Oct 2010 21:00:00 GMT",
    ISOString: "2010-10-31T21:00:00.000Z",
    LocaleString: "Mon, 11/01/2010, 12:00:00 AM GMT+3",
    LocaleDateString: "Mon, 11/01/2010",
    LocaleTimeString: "12:00:00 AM GMT+3",
  });
}

{
  // +04:00 (standard time)
  let local = new DateTime.Local(2011, Month.October, 30, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2011, Month.October, 29, DayOfWeek.Saturday, 20, 0, 0);

  assertDate(local, utc, TimeZone(+4), {
    String: "Sun Oct 30 2011 00:00:00 GMT+0400 (MSK)",
    DateString: "Sun Oct 30 2011",
    TimeString: "00:00:00 GMT+0400 (MSK)",
    UTCString: "Sat, 29 Oct 2011 20:00:00 GMT",
    ISOString: "2011-10-29T20:00:00.000Z",
    LocaleString: "Sun, 10/30/2011, 12:00:00 AM GMT+4",
    LocaleDateString: "Sun, 10/30/2011",
    LocaleTimeString: "12:00:00 AM GMT+4",
  });
}

{
  // +04:00 (standard time)
  let local = new DateTime.Local(2011, Month.November, 1, DayOfWeek.Tuesday, 0, 0, 0);
  let utc = new DateTime.UTC(2011, Month.October, 31, DayOfWeek.Monday, 20, 0, 0);

  assertDate(local, utc, TimeZone(+4), {
    String: "Tue Nov 01 2011 00:00:00 GMT+0400 (MSK)",
    DateString: "Tue Nov 01 2011",
    TimeString: "00:00:00 GMT+0400 (MSK)",
    UTCString: "Mon, 31 Oct 2011 20:00:00 GMT",
    ISOString: "2011-10-31T20:00:00.000Z",
    LocaleString: "Tue, 11/01/2011, 12:00:00 AM GMT+4",
    LocaleDateString: "Tue, 11/01/2011",
    LocaleTimeString: "12:00:00 AM GMT+4",
  });
}

// Russia changed timezone from +04:00 to +03:00 on 2014-10-26.

{
  // +04:00 (standard time)
  let local = new DateTime.Local(2014, Month.October, 26, DayOfWeek.Sunday, 0, 0, 0);
  let utc = new DateTime.UTC(2014, Month.October, 25, DayOfWeek.Saturday, 20, 0, 0);

  assertDate(local, utc, TimeZone(+4), {
    String: "Sun Oct 26 2014 00:00:00 GMT+0400 (MSK)",
    DateString: "Sun Oct 26 2014",
    TimeString: "00:00:00 GMT+0400 (MSK)",
    UTCString: "Sat, 25 Oct 2014 20:00:00 GMT",
    ISOString: "2014-10-25T20:00:00.000Z",
    LocaleString: "Sun, 10/26/2014, 12:00:00 AM GMT+4",
    LocaleDateString: "Sun, 10/26/2014",
    LocaleTimeString: "12:00:00 AM GMT+4",
  });
}

{
  // +03:00 (standard time)
  let local = new DateTime.Local(2014, Month.October, 27, DayOfWeek.Monday, 0, 0, 0);
  let utc = new DateTime.UTC(2014, Month.October, 26, DayOfWeek.Sunday, 21, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Mon Oct 27 2014 00:00:00 GMT+0300 (MSK)",
    DateString: "Mon Oct 27 2014",
    TimeString: "00:00:00 GMT+0300 (MSK)",
    UTCString: "Sun, 26 Oct 2014 21:00:00 GMT",
    ISOString: "2014-10-26T21:00:00.000Z",
    LocaleString: "Mon, 10/27/2014, 12:00:00 AM GMT+3",
    LocaleDateString: "Mon, 10/27/2014",
    LocaleTimeString: "12:00:00 AM GMT+3",
  });
}
