/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Liberia was the last country to switch to UTC based offsets (1972 May).

{
  let local = new DateTime.Local(1972, Month.April, 30, DayOfWeek.Sunday, 0, 0, 0);
  let utc = LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? new DateTime.UTC(1972, Month.April, 30, DayOfWeek.Sunday, 0, 44, 30)
  : new DateTime.UTC(1972, Month.April, 30, DayOfWeek.Sunday, 0, 0, 0);

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDate(local, utc, TimeZone(-0,44,30), {
      String: "Sun Apr 30 1972 00:00:00 GMT-0044 (LRT)",
      UTCString: "Sun, 30 Apr 1972 00:44:30 GMT",
    })
  : assertDate(local, utc, TimeZone(+0), {
      String: "Sun Apr 30 1972 00:00:00 GMT+0000 (GMT)",
      UTCString: "Sun, 30 Apr 1972 00:00:00 GMT",
    });
}

{
  let local = new DateTime.Local(1972, Month.April, 30, DayOfWeek.Sunday, 23, 59, 0);
  let utc = LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? new DateTime.UTC(1972, Month.May, 1, DayOfWeek.Monday, 0, 43, 30)
  : new DateTime.UTC(1972, Month.April, 30, DayOfWeek.Sunday, 23, 59, 0);

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDate(local, utc, TimeZone(-0,44,30), {
      String: "Sun Apr 30 1972 23:59:00 GMT-0044 (LRT)",
      UTCString: "Mon, 01 May 1972 00:43:30 GMT",
    })
  : assertDate(local, utc, TimeZone(+0), {
      String: "Sun Apr 30 1972 23:59:00 GMT+0000 (GMT)",
      UTCString: "Sun, 30 Apr 1972 23:59:00 GMT",
    });
}

{
  let local = new DateTime.Local(1972, Month.May, 1, DayOfWeek.Monday, 0, 0, 0);
  let utc = LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? new DateTime.UTC(1972, Month.May, 1, DayOfWeek.Monday, 0, 44, 30)
  : new DateTime.UTC(1972, Month.May, 1, DayOfWeek.Monday, 0, 0, 0);

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDateValue(local.toDate(), utc.toInstant(), TimeZone(+0).toOffset())
  : assertDateValue(local.toDate(), utc.toInstant(), TimeZone(+0).toOffset());

  LOCAL_TIME || LOCAL_TIME_AFTER_EPOCH
  ? assertDateString(local.toDate(), {
      String: "Mon May 01 1972 00:44:30 GMT+0000 (GMT)",
      UTCString: "Mon, 01 May 1972 00:44:30 GMT",
    })
  : assertDateString(local.toDate(), {
      String: "Mon May 01 1972 00:00:00 GMT+0000 (GMT)",
      UTCString: "Mon, 01 May 1972 00:00:00 GMT",
    });
}

{
  let local = new DateTime.Local(1972, Month.May, 1, DayOfWeek.Monday, 0, 44, 30);
  let utc = new DateTime.UTC(1972, Month.May, 1, DayOfWeek.Monday, 0, 44, 30);

  assertDate(local, utc, TimeZone(+0), {
    String: "Mon May 01 1972 00:44:30 GMT+0000 (GMT)",
    UTCString: "Mon, 01 May 1972 00:44:30 GMT",
  });
}

{
  let local = new DateTime.Local(1972, Month.May, 1, DayOfWeek.Monday, 0, 45, 0);
  let utc = new DateTime.UTC(1972, Month.May, 1, DayOfWeek.Monday, 0, 45, 0);

  assertDate(local, utc, TimeZone(+0), {
    String: "Mon May 01 1972 00:45:00 GMT+0000 (GMT)",
    UTCString: "Mon, 01 May 1972 00:45:00 GMT",
  });
}

{
  let local = new DateTime.Local(1972, Month.May, 2, DayOfWeek.Tuesday, 0, 0, 0);
  let utc = new DateTime.UTC(1972, Month.May, 2, DayOfWeek.Tuesday, 0, 0, 0);

  assertDate(local, utc, TimeZone(+0), {
    String: "Tue May 02 1972 00:00:00 GMT+0000 (GMT)",
    UTCString: "Tue, 02 May 1972 00:00:00 GMT",
  });
}
