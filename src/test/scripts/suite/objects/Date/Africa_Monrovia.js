/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
System.load("lib/assert-datetime.jsm");
System.load("lib/datetime.jsm");

const {
  assertDate,
  assertDateValue,
  assertDateString,
} = System.get("lib/assert-datetime.jsm");
const {
  DateTime, DayOfWeek, Month, TimeZone,
  LOCAL_TIME, LOCAL_TIME_AFTER_EPOCH,
} = System.get("lib/datetime.jsm");

// Liberia was the last country to switch to UTC based offsets (1972 May).

setTimeZone("Africa/Monrovia");

{
  let local = new DateTime.Local(1972, Month.January, 6, DayOfWeek.Thursday, 0, 0, 0);
  let utc = LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? new DateTime.UTC(1972, Month.January, 6, DayOfWeek.Thursday, 0, 44, 30)
  : new DateTime.UTC(1972, Month.January, 6, DayOfWeek.Thursday, 0, 0, 0);

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDate(local, utc, TimeZone(-0,44,30), {
      String: "Thu Jan 06 1972 00:00:00 GMT-0044 (MMT)",
      UTCString: "Thu, 06 Jan 1972 00:44:30 GMT",
    })
  : assertDate(local, utc, TimeZone(+0), {
      String: "Thu Jan 06 1972 00:00:00 GMT+0000 (GMT)",
      UTCString: "Thu, 06 Jan 1972 00:00:00 GMT",
    });
}

{
  let local = new DateTime.Local(1972, Month.January, 6, DayOfWeek.Thursday, 23, 59, 0);
  let utc = LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? new DateTime.UTC(1972, Month.January, 7, DayOfWeek.Friday, 0, 43, 30)
  : new DateTime.UTC(1972, Month.January, 6, DayOfWeek.Thursday, 23, 59, 0);

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDate(local, utc, TimeZone(-0,44,30), {
      String: "Thu Jan 06 1972 23:59:00 GMT-0044 (MMT)",
      UTCString: "Fri, 07 Jan 1972 00:43:30 GMT",
    })
  : assertDate(local, utc, TimeZone(+0), {
      String: "Thu Jan 06 1972 23:59:00 GMT+0000 (GMT)",
      UTCString: "Thu, 06 Jan 1972 23:59:00 GMT",
    });
}

{
  let local = new DateTime.Local(1972, Month.January, 7, DayOfWeek.Friday, 0, 0, 0);
  let utc = LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? new DateTime.UTC(1972, Month.January, 7, DayOfWeek.Friday, 0, 44, 30)
  : new DateTime.UTC(1972, Month.January, 7, DayOfWeek.Friday, 0, 0, 0);

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDateValue(local.toDate(), utc.toInstant(), TimeZone(+0).toOffset())
  : assertDateValue(local.toDate(), utc.toInstant(), TimeZone(+0).toOffset());

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDateString(local.toDate(), {
      String: "Fri Jan 07 1972 00:44:30 GMT+0000 (GMT)",
      UTCString: "Fri, 07 Jan 1972 00:44:30 GMT",
    })
  : assertDateString(local.toDate(), {
      String: "Fri Jan 07 1972 00:00:00 GMT+0000 (GMT)",
      UTCString: "Fri, 07 Jan 1972 00:00:00 GMT",
    });
}

{
  let local = new DateTime.Local(1972, Month.January, 7, DayOfWeek.Friday, 0, 44, 30);
  let utc = new DateTime.UTC(1972, Month.January, 7, DayOfWeek.Friday, 0, 44, 30);

  assertDate(local, utc, TimeZone(+0), {
    String: "Fri Jan 07 1972 00:44:30 GMT+0000 (GMT)",
    UTCString: "Fri, 07 Jan 1972 00:44:30 GMT",
  });
}

{
  let local = new DateTime.Local(1972, Month.January, 7, DayOfWeek.Friday, 0, 45, 0);
  let utc = new DateTime.UTC(1972, Month.January, 7, DayOfWeek.Friday, 0, 45, 0);

  assertDate(local, utc, TimeZone(+0), {
    String: "Fri Jan 07 1972 00:45:00 GMT+0000 (GMT)",
    UTCString: "Fri, 07 Jan 1972 00:45:00 GMT",
  });
}

{
  let local = new DateTime.Local(1972, Month.January, 8, DayOfWeek.Saturday, 0, 0, 0);
  let utc = new DateTime.UTC(1972, Month.January, 8, DayOfWeek.Saturday, 0, 0, 0);

  assertDate(local, utc, TimeZone(+0), {
    String: "Sat Jan 08 1972 00:00:00 GMT+0000 (GMT)",
    UTCString: "Sat, 08 Jan 1972 00:00:00 GMT",
  });
}
