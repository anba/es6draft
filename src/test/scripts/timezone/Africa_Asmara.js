/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Africa/Asmara as an example for mean time like timezones after LMT (AMT, ADMT).

{
  let local = new DateTime.Local(1889, Month.December, 31, DayOfWeek.Tuesday, 0, 0, 0);
  let utc = LOCAL_TIME
            ? new DateTime.UTC(1889, Month.December, 30, DayOfWeek.Monday, 21, 24, 28)
            : new DateTime.UTC(1889, Month.December, 30, DayOfWeek.Monday, 21, 0, 0);

  LOCAL_TIME
  ? assertDate(local, utc, TimeZone(+2,35,32), {
      String: "Tue Dec 31 1889 00:00:00 GMT+0235 (AMT)",
      UTCString: "Mon, 30 Dec 1889 21:24:28 GMT",
    })
  : assertDate(local, utc, TimeZone(+3), {
      String: "Tue Dec 31 1889 00:00:00 GMT+0300 (EAT)",
      UTCString: "Mon, 30 Dec 1889 21:00:00 GMT",
    });
}

{
  let local = new DateTime.Local(1936, Month.May, 4, DayOfWeek.Monday, 0, 0, 0);
  let utc = LOCAL_TIME
            ? new DateTime.UTC(1936, Month.May, 3, DayOfWeek.Sunday, 21, 24, 40)
            : new DateTime.UTC(1936, Month.May, 3, DayOfWeek.Sunday, 21, 0, 0);

  LOCAL_TIME
  ? assertDate(local, utc, TimeZone(+2,35,20), {
      String: "Mon May 04 1936 00:00:00 GMT+0235 (ADMT)",
      UTCString: "Sun, 03 May 1936 21:24:40 GMT",
    })
  : assertDate(local, utc, TimeZone(+3), {
      String: "Mon May 04 1936 00:00:00 GMT+0300 (EAT)",
      UTCString: "Sun, 03 May 1936 21:00:00 GMT",
    });
}

{
  let local = new DateTime.Local(1936, Month.May, 6, DayOfWeek.Wednesday, 0, 0, 0);
  let utc = new DateTime.UTC(1936, Month.May, 5, DayOfWeek.Tuesday, 21, 0, 0);

  assertDate(local, utc, TimeZone(+3), {
    String: "Wed May 06 1936 00:00:00 GMT+0300 (EAT)",
    UTCString: "Tue, 05 May 1936 21:00:00 GMT",
  });
}
