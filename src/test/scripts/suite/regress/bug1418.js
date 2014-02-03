/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

// 11.8.1: Missing ToBoolean conversion in instanceofOperator
// https://bugs.ecmascript.org/show_bug.cgi?id=1418

let falsy = [void 0, null, 0, false, ""];
let truthy = [1, true, "abc", {}];

for (let v of falsy) {
  assertFalse(null instanceof {[Symbol.hasInstance]() { return v }});
}

for (let v of truthy) {
  assertTrue(null instanceof {[Symbol.hasInstance]() { return v }});
}
