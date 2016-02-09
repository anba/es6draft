/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// Characters with multiple case fold equivalent characters
let map = [
  ["\u00b5", "\u03bc"],
  ["\u01c4", "\u01c5"],
  ["\u01c6", "\u01c5"],
  ["\u01c7", "\u01c8"],
  ["\u01c9", "\u01c8"],
  ["\u01ca", "\u01cb"],
  ["\u01cc", "\u01cb"],
  ["\u01f1", "\u01f2"],
  ["\u01f3", "\u01f2"],
  ["\u0345", "\u03b9"],
  ["\u0345", "\u1fbe"],
  ["\u0392", "\u03d0"],
  ["\u0395", "\u03f5"],
  ["\u0398", "\u03d1"],
  ["\u0399", "\u0345"],
  ["\u0399", "\u1fbe"],
  ["\u039a", "\u03f0"],
  ["\u039c", "\u00b5"],
  ["\u03a0", "\u03d6"],
  ["\u03a1", "\u03f1"],
  ["\u03a3", "\u03c2"],
  ["\u03a6", "\u03d5"],
  ["\u03b2", "\u03d0"],
  ["\u03b5", "\u03f5"],
  ["\u03b8", "\u03d1"],
  ["\u03b9", "\u0345"],
  ["\u03b9", "\u1fbe"],
  ["\u03ba", "\u03f0"],
  ["\u03bc", "\u00b5"],
  ["\u03c0", "\u03d6"],
  ["\u03c1", "\u03f1"],
  ["\u03c2", "\u03c3"],
  ["\u03c3", "\u03c2"],
  ["\u03c6", "\u03d5"],
  ["\u03d0", "\u03b2"],
  ["\u03d1", "\u03b8"],
  ["\u03d5", "\u03c6"],
  ["\u03d6", "\u03c0"],
  ["\u03f0", "\u03ba"],
  ["\u03f1", "\u03c1"],
  ["\u03f5", "\u03b5"],
  ["\u1e60", "\u1e9b"],
  ["\u1e61", "\u1e9b"],
  ["\u1e9b", "\u1e61"],
  ["\u1fbe", "\u0345"],
  ["\u1fbe", "\u03b9"],
];

for (let [from, to] of map) {
  assertTrue(new RegExp(`^${from}$`, "i").test(to));
  assertTrue(new RegExp(`^${to}$`, "i").test(from));

  assertTrue(new RegExp(`^[${from}]$`, "i").test(to));
  assertTrue(new RegExp(`^[${to}]$`, "i").test(from));

  assertFalse(new RegExp(`^[^${from}]$`, "i").test(to));
  assertFalse(new RegExp(`^[^${to}]$`, "i").test(from));
}
