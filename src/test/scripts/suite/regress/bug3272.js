/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue
} = Assert;

// 7.1.16 CanonicalNumericIndexString: Consider to ignore canonical, but non-integer or negative property keys in integer indexed objects?
// https://bugs.ecmascript.org/show_bug.cgi?id=3272

var ta = new Int8Array(10);

var canonicalNumericIndices = [
  "NaN", "Infinity", "-Infinity", "1e+21", "1e-10", "0.1", "9999999999999998"
];
var invalidNumericIndices = [
  "+Infinity", "1e21", "1e+20", "1e-6", "0.10", "9999999999999999"
];

var desc = {value: 0, writable: true, enumerable: true, configurable: false};
assertTrue(Reflect.defineProperty(ta, "0", desc));

for (var index of canonicalNumericIndices) {
  assertFalse(Reflect.defineProperty(ta, index, desc), `ìndex=${index}`);
}

for (var index of invalidNumericIndices) {
  assertTrue(Reflect.defineProperty(ta, index, desc), `ìndex=${index}`);
}
