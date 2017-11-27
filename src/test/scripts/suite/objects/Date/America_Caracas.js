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

// America/Caracas switched from -04:00 to -04:30 on 2007 Dec 9.

setTimeZone("America/Caracas");

{
  // -04:00 (standard time)
  let local = new DateTime.Local(2007, Month.December, 5, DayOfWeek.Wednesday, 0, 0, 0);
  let utc = new DateTime.UTC(2007, Month.December, 5, DayOfWeek.Wednesday, 4, 0, 0);

  assertDate(local, utc, TimeZone(-4), {
    String: "Wed Dec 05 2007 00:00:00 GMT-0400 (-04)",
    DateString: "Wed Dec 05 2007",
    TimeString: "00:00:00 GMT-0400 (-04)",
    UTCString: "Wed, 05 Dec 2007 04:00:00 GMT",
    ISOString: "2007-12-05T04:00:00.000Z",
    LocaleString: "Wed, 12/05/2007, 12:00:00 AM GMT-4",
    LocaleDateString: "Wed, 12/05/2007",
    LocaleTimeString: "12:00:00 AM GMT-4",
  });
}

{
  // -04:30 (standard time)
  let local = new DateTime.Local(2007, Month.December, 12, DayOfWeek.Wednesday, 0, 0, 0);
  let utc = new DateTime.UTC(2007, Month.December, 12, DayOfWeek.Wednesday, 4, 30, 0);

  assertDate(local, utc, TimeZone(-4, 30), {
    String: "Wed Dec 12 2007 00:00:00 GMT-0430 (-0430)",
    DateString: "Wed Dec 12 2007",
    TimeString: "00:00:00 GMT-0430 (-0430)",
    UTCString: "Wed, 12 Dec 2007 04:30:00 GMT",
    ISOString: "2007-12-12T04:30:00.000Z",
    LocaleString: "Wed, 12/12/2007, 12:00:00 AM GMT-4:30",
    LocaleDateString: "Wed, 12/12/2007",
    LocaleTimeString: "12:00:00 AM GMT-4:30",
  });
}
