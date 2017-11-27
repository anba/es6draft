/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue,
} = Assert;

// 5.2 Algorithm Conventions
function modulo(dividend, divisor) {
  assertTrue(typeof dividend === "number");
  assertTrue(typeof divisor === "number");
  assertTrue(divisor !== 0 && Number.isFinite(divisor));
  let remainder = dividend % divisor;
  // NB: add +0 to convert -0 to +0
  return (remainder >= 0 ? remainder + 0 : remainder + divisor);
}

// 7.1.4 ToInteger ( argument )
function ToInteger(number) {
  /* steps 1-2 */
  assertTrue(typeof number === "number");
  /* step 3 */
  if (Number.isNaN(number))
    return +0.0;
  /* step 4 */
  if (number == 0.0 || !Number.isFinite(number))
    return number;
  /* step 5 */
  return Math.sign(number) * Math.floor(Math.abs(number));
}

// 20.3.1.2 Day Number and Time within Day
const msPerDay = 86400000;

// 20.3.1.2 Day Number and Time within Day
function Day(t) {
  assertTrue(typeof t === "number");
  return Math.floor(t / msPerDay);
}

// 20.3.1.2 Day Number and Time within Day
function TimeWithinDay(t) {
  assertTrue(typeof t === "number");
  return modulo(t, msPerDay);
}

// 20.3.1.3 Year Number
function DaysInYear(y) {
  assertTrue(typeof y === "number");
  if (y % 4 !== 0) {
    return 365;
  }
  if (y % 100 !== 0) {
    return 366;
  }
  if (y % 400 !== 0) {
    return 365;
  }
  return 366;
}

// 20.3.1.3 Year Number
function DayFromYear(y) {
  assertTrue(typeof y === "number");
  return 365 * (y - 1970) + Math.floor((y - 1969) / 4) - Math.floor((y - 1901) / 100) + Math.floor((y - 1601) / 400);
}

// 20.3.1.3 Year Number
function TimeFromYear(y) {
  assertTrue(typeof y === "number");
  return msPerDay * DayFromYear(y);
}

// TODO: fill in rest

// 20.3.1.10 Hours, Minutes, Second, and Milliseconds
const HoursPerDay = 24;
const MinutesPerHour = 60;
const SecondsPerMinute = 60;
const msPerSecond = 1000;
const msPerMinute = msPerSecond * SecondsPerMinute;
const msPerHour = msPerMinute * MinutesPerHour;

// 20.3.1.10 Hours, Minutes, Second, and Milliseconds
function HourFromTime(t) {
  assertTrue(typeof t === "number");
  return modulo(Math.floor(t / msPerHour), HoursPerDay);
}

// 20.3.1.10 Hours, Minutes, Second, and Milliseconds
function MinFromTime(t) {
  assertTrue(typeof t === "number");
  return modulo(Math.floor(t / msPerMinute), MinutesPerHour);
}

// 20.3.1.10 Hours, Minutes, Second, and Milliseconds
function SecFromTime(t) {
  assertTrue(typeof t === "number");
  return modulo(Math.floor(t / msPerSecond), SecondsPerMinute);
}

// 20.3.1.10 Hours, Minutes, Second, and Milliseconds
function msFromTime(t) {
  assertTrue(typeof t === "number");
  return modulo(t, msPerSecond);
}

// 20.3.1.11 MakeTime (hour, min, sec, ms)
function MakeTime(hour, min, sec, ms) {
  assertTrue(typeof hour === "number");
  assertTrue(typeof min === "number");
  assertTrue(typeof sec === "number");
  assertTrue(typeof ms === "number");
  if (!Number.isFinite(hour) || !Number.isFinite(min) || !Number.isFinite(sec) || !Number.isFinite(ms)) {
    return Number.NaN;
  }
  let h = ToInteger(hour);
  let m = ToInteger(min);
  let s = ToInteger(sec);
  let milli = ToInteger(ms);
  let t = h * msPerHour + m * msPerMinute + s * msPerSecond + milli;
  return t;
}

// 20.3.1.12 MakeDay (year, month, date)
function MakeDay(year, month, date) {
  assertTrue(typeof year === "number");
  assertTrue(typeof month === "number");
  assertTrue(typeof date === "number");
  if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(date)) {
    return Number.NaN;
  }
  let y = ToInteger(year);
  let m = ToInteger(month);
  let dt = ToInteger(date);
  let ym = y + Math.floor(m / 12);
  let mn = modulo(m, 12);

  const monthStart = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
  let day = Math.floor(TimeFromYear(ym) / msPerDay) + monthStart[mn];
  if (mn >= 2 && DaysInYear(ym) == 366) {
    day += 1;
  }

  return day + dt - 1;
}

// 20.3.1.13 MakeDate (day, time)
function MakeDate(day, time) {
  assertTrue(typeof day === "number");
  assertTrue(typeof time === "number");
  if (!Number.isFinite(day) || !Number.isFinite(time)) {
    return Number.NaN;
  }
  return day * msPerDay + time;
}

// 20.3.1.14 TimeClip (time)
function TimeClip(time) {
  assertTrue(typeof time === "number");
  if (!Number.isFinite(time)) {
    return Number.NaN;
  }
  if (Math.abs(time) > 8.64e15) {
    return Number.NaN;
  }
  return ToInteger(time) + (+0);
}

// Flags to enable/disable support for local time precision.
export const LOCAL_TIME = false;
export const LOCAL_TIME_AFTER_EPOCH = true;

export const DayOfWeek = {
  Sunday: 0,
  Monday: 1,
  Tuesday: 2,
  Wednesday: 3,
  Thursday: 4,
  Friday: 5,
  Saturday: 6,
};

export const Month = {
  January: 0,
  February: 1,
  March: 2,
  April: 3,
  May: 4,
  June: 5,
  July: 6,
  August: 7,
  September: 8,
  October: 9,
  November: 10,
  December: 11,
};

export const DateTime = {
  Local: class {
    constructor(year, month, day, weekday, hour = 0, minute = 0, second = 0, ms = 0) {
      Object.assign(this, {year, month, day, weekday, hour, minute, second, ms});
    }

    toDate() {
      return new Date(this.year, this.month, this.day, this.hour, this.minute, this.second, this.ms);
    }
  },
  UTC: class {
    constructor(year, month, day, weekday, hour = 0, minute = 0, second = 0, ms = 0) {
      Object.assign(this, {year, month, day, weekday, hour, minute, second, ms});
    }

    toInstant() {
      return MakeDate(MakeDay(this.year, this.month, this.day), MakeTime(this.hour, this.minute, this.second, this.ms));
    }
  },
};

export function TimeZone(hour, minute = 0, second = 0) {
  return new class TimeZone {
    constructor(hour, minute, second) {
      Object.assign(this, {hour, minute, second});
    }

    toOffset() {
      let offset = TimeZoneOffset(this.hour, this.minute, this.second);
      return offset !== 0 ? -offset : 0;
    }
  }(hour, minute, second);

  function TimeZoneOffset(hour, minute = 0, second = 0) {
    assertTrue(typeof hour === "number");
    assertTrue(typeof minute === "number");
    assertTrue(typeof second === "number");
    assertTrue(minute >= 0);
    assertTrue(second >= 0);
    if (hour < 0 || Object.is(-0, hour)) {
      return hour * MinutesPerHour - minute - (second / 60);
    }
    return hour * MinutesPerHour + minute + (second / 60);
  }
}

export const Format = {
  Locale: "en-US",
  DateTime: {
    localeMatcher: "lookup",
    timeZone: void 0,
    weekday: "short",
    era: void 0,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    timeZoneName: "short",
    formatMatcher: "best fit",
    hour12: void 0,
  },
  Date: {
    localeMatcher: "lookup",
    timeZone: void 0,
    weekday: "short",
    era: void 0,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: void 0,
    minute: void 0,
    second: void 0,
    timeZoneName: void 0,
    formatMatcher: "best fit",
    hour12: void 0,
  },
  Time: {
    localeMatcher: "lookup",
    timeZone: void 0,
    weekday: void 0,
    era: void 0,
    year: void 0,
    month: void 0,
    day: void 0,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    timeZoneName: "short",
    formatMatcher: "best fit",
    hour12: void 0,
  },
};
