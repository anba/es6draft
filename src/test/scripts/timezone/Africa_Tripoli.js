/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Africa/Tripoli switched from +02:00 to +01:00 and back.

{
  // +02:00 (standard time)
  let local = new DateTime.Local(2012, Month.November, 1, DayOfWeek.Thursday, 0, 0, 0);
  let utc = new DateTime.UTC(2012, Month.October, 31, DayOfWeek.Wednesday, 22, 0, 0);

  assertDate(local, utc, TimeZone(+2), {
    String: "Thu Nov 01 2012 00:00:00 GMT+0200 (EET)",
    UTCString: "Wed, 31 Oct 2012 22:00:00 GMT",
  });
}

{
  // +01:00 (standard time)
  let local = new DateTime.Local(2012, Month.December, 1, DayOfWeek.Saturday, 0, 0, 0);
  let utc = new DateTime.UTC(2012, Month.November, 30, DayOfWeek.Friday, 23, 0, 0);

  assertDate(local, utc, TimeZone(+1), {
    String: "Sat Dec 01 2012 00:00:00 GMT+0100 (CET)",
    UTCString: "Fri, 30 Nov 2012 23:00:00 GMT",
  });
}

{
  // +01:00 (daylight savings)
  let local = new DateTime.Local(2013, Month.October, 1, DayOfWeek.Tuesday, 0, 0, 0);
  let utc = new DateTime.UTC(2013, Month.September, 30, DayOfWeek.Monday, 22, 0, 0);

  assertDate(local, utc, TimeZone(+2), {
    String: "Tue Oct 01 2013 00:00:00 GMT+0200 (CEST)",
    UTCString: "Mon, 30 Sep 2013 22:00:00 GMT",
  });
}

{
  // +02:00 (standard time)
  let local = new DateTime.Local(2013, Month.November, 1, DayOfWeek.Friday, 0, 0, 0);
  let utc = new DateTime.UTC(2013, Month.October, 31, DayOfWeek.Thursday, 22, 0, 0);

  assertDate(local, utc, TimeZone(+2), {
    String: "Fri Nov 01 2013 00:00:00 GMT+0200 (EET)",
    UTCString: "Thu, 31 Oct 2013 22:00:00 GMT",
  });
}
