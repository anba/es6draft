/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

// Re-export assert module as global property "Assert".
Object.defineProperty(this, "Assert", {value: System.get("../suite/lib/assert.jsm")});

function assertDate(local, utc, timeZone, options, formatArgs) {
  let d = local.toDate();
  assertDateValue(d, utc.toInstant(), timeZone.toOffset());
  assertLocalDate(d, local);
  assertUTCDate(d, utc);
  assertDateString(d, options, formatArgs);
}

function assertDateValue(actual, dateValue, timeZoneOffset) {
  Assert.assertSame(dateValue, actual.valueOf(), `valueOf()[${dateValue - actual.valueOf()}]`);
  Assert.assertSame(dateValue, actual.getTime(), `valueOf()[${dateValue - actual.getTime()}]`);
  Assert.assertSame(timeZoneOffset, actual.getTimezoneOffset(), "getTimezoneOffset()");
}

function assertLocalDate(actual, {year, month, day, weekday, hour = 0, minute = 0, second = 0, ms = 0}) {
  Assert.assertSame(year, actual.getFullYear(), "getFullYear()");
  Assert.assertSame(month, actual.getMonth(), "getMonth()");
  Assert.assertSame(day, actual.getDate(), "getDate()");
  Assert.assertSame(weekday, actual.getDay(), "getDay()");
  Assert.assertSame(hour, actual.getHours(), "getHours()");
  Assert.assertSame(minute, actual.getMinutes(), "getMinutes()");
  Assert.assertSame(second, actual.getSeconds(), "getSeconds()");
  Assert.assertSame(ms, actual.getMilliseconds(), "getMilliseconds()");
}

function assertUTCDate(actual, {year, month, day, weekday, hour = 0, minute = 0, second = 0, ms = 0}) {
  Assert.assertSame(year, actual.getUTCFullYear(), "getUTCFullYear()");
  Assert.assertSame(month, actual.getUTCMonth(), "getUTCMonth()");
  Assert.assertSame(day, actual.getUTCDate(), "getUTCDate()");
  Assert.assertSame(weekday, actual.getUTCDay(), "getUTCDay()");
  Assert.assertSame(hour, actual.getUTCHours(), "getUTCHours()");
  Assert.assertSame(minute, actual.getUTCMinutes(), "getUTCMinutes()");
  Assert.assertSame(second, actual.getUTCSeconds(), "getUTCSeconds()");
  Assert.assertSame(ms, actual.getUTCMilliseconds(), "getUTCMilliseconds()");
}

function assertDateString(actual, options, formatArgs = {
  LocaleString: [Format.Locale, Format.DateTime],
  LocaleDateString: [Format.Locale, Format.Date],
  LocaleTimeString: [Format.Locale, Format.Time],
}) {
  for (var key of Object.keys(options)) {
    var args = formatArgs[key] || [];
    Assert.assertSame(options[key], actual[`to${key}`](...args), `to${key}()`);
  }
}
