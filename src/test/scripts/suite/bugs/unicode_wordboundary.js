/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue, assertFalse
} = Assert;

// 00B5;MICRO SIGN;Ll;
// 00C0;LATIN CAPITAL LETTER A WITH GRAVE;Lu;
// 0660;ARABIC-INDIC DIGIT ZERO;Nd;
// 104A0;OSMANYA DIGIT ZERO;Nd;

// Word boundary detection works correctly in non-unicode mode
assertTrue(/A\b/.test("A\u00B5"));
assertFalse(/A\B/.test("A\u00B5"));
assertTrue(/A\b/.test("A\u00C0"));
assertFalse(/A\B/.test("A\u00C0"));
assertTrue(/A\b/.test("A\u0660"));
assertFalse(/A\B/.test("A\u0660"));
assertTrue(/A\b/.test("A\u{104A0}"));
assertFalse(/A\B/.test("A\u{104A0}"));


// Word boundary detection incorrectly accepts digits and letters outside of [0-9A-Za-z] in unicode mode
assertFalse(/A\b/u.test("A\u00B5"));
assertTrue(/A\B/u.test("A\u00B5"));
assertFalse(/A\b/u.test("A\u00C0"));
assertTrue(/A\B/u.test("A\u00C0"));
assertFalse(/A\b/u.test("A\u0660"));
assertTrue(/A\B/u.test("A\u0660"));
