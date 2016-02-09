/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 12.1.1.1 ToDateTimeOptions: Change Throw parameter to true
// https://bugs.ecmascript.org/show_bug.cgi?id=3399

var options = {get second() {
  Object.defineProperty(this, "year", {value: "2-digit"});
}};
assertThrows(TypeError, () => new Intl.DateTimeFormat("de", options).format(new Date));
