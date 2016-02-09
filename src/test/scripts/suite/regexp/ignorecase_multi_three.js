/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// Characters with three case fold equivalent characters
const chars = [
  // LATIN CAPITAL LETTER DZ WITH CARON
  // LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON
  // LATIN SMALL LETTER DZ WITH CARON
  ["\u01c4", "\u01c5", "\u01c6"],

  // LATIN CAPITAL LETTER LJ
  // LATIN CAPITAL LETTER L WITH SMALL LETTER J
  // LATIN SMALL LETTER LJ
  ["\u01c7", "\u01c8", "\u01c9"],

  // LATIN CAPITAL LETTER NJ
  // LATIN CAPITAL LETTER N WITH SMALL LETTER J
  // LATIN SMALL LETTER NJ
  ["\u01ca", "\u01cb", "\u01cc"],

  // LATIN CAPITAL LETTER DZ
  // LATIN CAPITAL LETTER D WITH SMALL LETTER Z
  // LATIN SMALL LETTER DZ
  ["\u01f1", "\u01f2", "\u01f3"],

  // GREEK CAPITAL LETTER SIGMA
  // GREEK SMALL LETTER FINAL SIGMA
  // GREEK SMALL LETTER SIGMA
  ["\u03a3", "\u03c2", "\u03c3"],

  // GREEK CAPITAL LETTER MU
  // GREEK SMALL LETTER MU
  // MICRO SIGN
  ["\u039c", "\u03bc", "\u00b5"],
];

for (let [s1, s2, s3] of chars) {
  for (let s of [s1, s2, s3]) {
    for (let r of [s1, s2, s3]) {
      assertTrue(new RegExp(r, "i").test(s), `r=${r.codePointAt(0).toString(16)}, s=${s.codePointAt(0).toString(16)}`);
      assertTrue(new RegExp(`[${r}]`, "i").test(s), `r=${r.codePointAt(0).toString(16)}, s=${s.codePointAt(0).toString(16)}`);
      assertFalse(new RegExp(`[^${r}]`, "i").test(s), `r=${r.codePointAt(0).toString(16)}, s=${s.codePointAt(0).toString(16)}`);
    }
  }
  // Test back references
  for (let s of [s1, s2, s3]) {
    assertTrue(new RegExp(`^(.)\\1\\1\\1$`, "i").test(s + s1 + s2 + s3));
    assertTrue(new RegExp(`^(${s})\\1\\1\\1$`, "i").test(s + s1 + s2 + s3));
    assertTrue(new RegExp(`^([${s}])\\1\\1\\1$`, "i").test(s + s1 + s2 + s3));
  }
}
