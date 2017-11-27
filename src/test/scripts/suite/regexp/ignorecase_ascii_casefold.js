/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// Characters where ToUpper(codePoint_1) == ToUpper(codePoint_2) and IsASCII(codePoint_1) ^ IsASCII(codePoint_2)
let map = [
  ["\u0049", "\u0131"],
  ["\u0053", "\u017f"],
  ["\u0069", "\u0131"],
  ["\u0073", "\u017f"],
  ["\u0131", "\u0069"],
  ["\u017f", "\u0073"],
];

for (let [from, to] of map) {
  assertFalse(new RegExp(`^${from}$`, "i").test(to));
  assertFalse(new RegExp(`^${to}$`, "i").test(from));

  assertFalse(new RegExp(`^[${from}]$`, "i").test(to));
  assertFalse(new RegExp(`^[${to}]$`, "i").test(from));

  assertTrue(new RegExp(`^[^${from}]$`, "i").test(to));
  assertTrue(new RegExp(`^[^${to}]$`, "i").test(from));
}
