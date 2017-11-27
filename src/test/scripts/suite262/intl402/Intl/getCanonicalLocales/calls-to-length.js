/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Calls ToLength on the "length" property.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    ...
    5. Let len be ? ToLength(? Get(O, "length")).
    ...
---*/

assert.sameValue(Intl.getCanonicalLocales({length: -1, 0: "<invalid>"}).length, 0, "length is -1");
assert.sameValue(Intl.getCanonicalLocales({length: 0.8, 0: "<invalid>"}).length, 0, "length is 0.8");
assert.sameValue(Intl.getCanonicalLocales({length: 1.8, 0: "de", 1: "<invalid>"}).length, 1, "length is 1.8");
assert.sameValue(Intl.getCanonicalLocales({length: "1.6", 0: "de", 1: "<invalid>"}).length, 1, "length is '1.6'");
