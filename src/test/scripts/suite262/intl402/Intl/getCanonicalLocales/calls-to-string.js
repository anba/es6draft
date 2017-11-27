/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Calls ToString on the locales' elements.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    ...
    7. Repeat, while k < len
      a. Let Pk be ToString(k).
      b. Let kPresent be ? HasProperty(O, Pk).
      c. If kPresent is true, then
        i. Let kValue be ? Get(O, Pk).
        ...
        iii. Let tag be ? ToString(kValue).
        ...
---*/

var callCount = 0;

var locale = {
  toString: function() {
    callCount += 1;
    return "pt-br";
  },
  valueOf: function() {
    $ERROR("valueOf() called");
  }
};

var canonicalLocales = Intl.getCanonicalLocales([locale]);
assert.sameValue(canonicalLocales.length, 1);
assert.sameValue(canonicalLocales[0], "pt-BR");
assert.sameValue(callCount, 1);
