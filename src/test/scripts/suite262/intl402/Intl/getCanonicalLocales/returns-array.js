/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Returns an array object.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    1. If locales is undefined, then
      a. Return a new empty List.
    ...

  7.3.16 CreateArrayFromList (elements)
    1. Assert: elements is a List whose elements are all ECMAScript language values.
    2. Let array be ArrayCreate(0).
    ...
    5. Return array.
---*/

var canonicalLocales = Intl.getCanonicalLocales();

assert(Array.isArray(canonicalLocales));
assert(Object.isExtensible(canonicalLocales));
