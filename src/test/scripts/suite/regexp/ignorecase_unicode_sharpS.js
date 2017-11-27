/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// Characters where ToUpper(codePoint) == CaseFold(codePoint)
// and exists codePoint2 with:
//   codePoint2 != codePoint and CaseFold(codePoint) == CaseFold(codePoint2)
let map = [
  ["\u00df", "\u1e9e"],
];

for (let [from, to] of map) {
  assertTrue(new RegExp(`^${from}$`, "iu").test(to));
  assertTrue(new RegExp(`^${to}$`, "iu").test(from));

  assertTrue(new RegExp(`^[${from}]$`, "iu").test(to));
  assertTrue(new RegExp(`^[${to}]$`, "iu").test(from));

  assertFalse(new RegExp(`^[^${from}]$`, "iu").test(to));
  assertFalse(new RegExp(`^[^${to}]$`, "iu").test(from));
}
