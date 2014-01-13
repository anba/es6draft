/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue, assertFalse
} = Assert;

// surrogate detection for unescaped and escaped code units
assertTrue(RegExp("\ud801\udc00", "u").test("\u{10400}"));
assertTrue(RegExp("\\ud801\\udc00", "u").test("\u{10400}"));

// mixed code units are not treated as a single code point
assertFalse(RegExp("\\ud801\udc00", "u").test("\u{10400}"));
assertFalse(RegExp("\ud801\\udc00", "u").test("\u{10400}"));
