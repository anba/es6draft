/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

{
  // +01:00 (standard time)
  let local = new DateTime.Local(1970, Month.January, 1, DayOfWeek.Thursday, 0, 0, 0);
  let utc = new DateTime.UTC(1969, Month.December, 31, DayOfWeek.Wednesday, 23, 0, 0);

  assertDate(local, utc, TimeZone(+1), {
    String: "Thu Jan 01 1970 00:00:00 GMT+0100 (BST)",
    DateString: "Thu Jan 01 1970",
    TimeString: "00:00:00 GMT+0100 (BST)",
    UTCString: "Wed, 31 Dec 1969 23:00:00 GMT",
    ISOString: "1969-12-31T23:00:00.000Z",
    LocaleString: "Thu, 01/01/1970, 12:00:00 AM GMT+1",
    LocaleDateString: "Thu, 01/01/1970",
    LocaleTimeString: "12:00:00 AM GMT+1",
  });
}
