/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Returns an empty array if the `locales` parameter is `undefined`.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    1. If locales is undefined, then
      a. Return a new empty List.
    ...
---*/

var canonicalLocales = Intl.getCanonicalLocales(undefined);

assert.sameValue(canonicalLocales.length, 0);
