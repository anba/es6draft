/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// Characters where ToUpper(codePoint) != ToUpper(ToLower(codePoint))
let map = [
  ["\u0130", "\u0069"],
  ["\u03f4", "\u03b8"],
  ["\u1e9e", "\u00df"],
  ["\u2126", "\u03c9"],
  ["\u212a", "\u006b"],
  ["\u212b", "\u00e5"],
];

for (let [codePoint, toLower] of map) {
  assertFalse(new RegExp(`^${codePoint}$`, "i").test(toLower));
  assertFalse(new RegExp(`^${toLower}$`, "i").test(codePoint));

  assertFalse(new RegExp(`^[${codePoint}]$`, "i").test(toLower));
  assertFalse(new RegExp(`^[${toLower}]$`, "i").test(codePoint));

  assertTrue(new RegExp(`^[^${codePoint}]$`, "i").test(toLower));
  assertTrue(new RegExp(`^[^${toLower}]$`, "i").test(codePoint));
}
