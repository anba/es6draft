/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Language tag must either be a string or an object.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    ...

  9.2.1 CanonicalizeLocaleList (locales)
    ...
    7. Repeat, while k < len
      ...
      c. If kPresent is true, then
        ...
        ii. If Type(kValue) is not String or Object, throw a TypeError exception.
        ...
features: [Symbol]
---*/

assert.throws(TypeError, function() {
  Intl.getCanonicalLocales([undefined]);
}, "language tag is `undefined`");

assert.throws(TypeError, function() {
  Intl.getCanonicalLocales([null]);
}, "language tag is `null`");

assert.throws(TypeError, function() {
  Intl.getCanonicalLocales([true]);
}, "language tag is a boolean");

assert.throws(TypeError, function() {
  Intl.getCanonicalLocales([0]);
}, "language tag is a number (0)");

assert.throws(TypeError, function() {
  Intl.getCanonicalLocales([NaN]);
}, "language tag is a number (NaN)");

assert.throws(TypeError, function() {
  Intl.getCanonicalLocales([Symbol()]);
}, "language tag is a symbol");
