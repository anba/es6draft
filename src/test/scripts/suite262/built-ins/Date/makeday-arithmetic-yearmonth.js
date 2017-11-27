/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-makeday
description: |
  Test arithmetic in MakeDay abstract operation.
info: >
  20.3.1.13 MakeDay (year, month, date)
    ...
    2. Let y be ToInteger(year).
    3. Let m be ToInteger(month).
    ...
    5. Let ym be y + floor(m / 12).
    ...
---*/

var t = 6000000;
var year = 2016, month = 0, day = 1;
var date = new Date(year + t, month + -(t * 12), day);

assert.sameValue(date.valueOf(), new Date(year, month, day).valueOf());
