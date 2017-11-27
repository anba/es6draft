/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
import { Format } from "./datetime.jsm";

const {
  assertSame,
} = Assert;

export function assertDate(local, utc, timeZone, options, formatArgs) {
  let d = local.toDate();
  assertDateValue(d, utc.toInstant(), timeZone.toOffset());
  assertLocalDate(d, local);
  assertUTCDate(d, utc);
  assertDateString(d, options, formatArgs);
}

export function assertDateValue(actual, dateValue, timeZoneOffset) {
  assertSame(dateValue, actual.valueOf(), `valueOf()[${dateValue - actual.valueOf()}]`);
  assertSame(dateValue, actual.getTime(), `valueOf()[${dateValue - actual.getTime()}]`);
  assertSame(timeZoneOffset, actual.getTimezoneOffset(), "getTimezoneOffset()");
}

export function assertLocalDate(actual, {year, month, day, weekday, hour = 0, minute = 0, second = 0, ms = 0}) {
  assertSame(year, actual.getFullYear(), "getFullYear()");
  assertSame(month, actual.getMonth(), "getMonth()");
  assertSame(day, actual.getDate(), "getDate()");
  assertSame(weekday, actual.getDay(), "getDay()");
  assertSame(hour, actual.getHours(), "getHours()");
  assertSame(minute, actual.getMinutes(), "getMinutes()");
  assertSame(second, actual.getSeconds(), "getSeconds()");
  assertSame(ms, actual.getMilliseconds(), "getMilliseconds()");
}

export function assertUTCDate(actual, {year, month, day, weekday, hour = 0, minute = 0, second = 0, ms = 0}) {
  assertSame(year, actual.getUTCFullYear(), "getUTCFullYear()");
  assertSame(month, actual.getUTCMonth(), "getUTCMonth()");
  assertSame(day, actual.getUTCDate(), "getUTCDate()");
  assertSame(weekday, actual.getUTCDay(), "getUTCDay()");
  assertSame(hour, actual.getUTCHours(), "getUTCHours()");
  assertSame(minute, actual.getUTCMinutes(), "getUTCMinutes()");
  assertSame(second, actual.getUTCSeconds(), "getUTCSeconds()");
  assertSame(ms, actual.getUTCMilliseconds(), "getUTCMilliseconds()");
}

export function assertDateString(actual, options, formatArgs = {
  LocaleString: [Format.Locale, Format.DateTime],
  LocaleDateString: [Format.Locale, Format.Date],
  LocaleTimeString: [Format.Locale, Format.Time],
}) {
  for (var key of Object.keys(options)) {
    var args = formatArgs[key] || [];
    assertSame(options[key], actual[`to${key}`](...args), `to${key}()`);
  }
}
