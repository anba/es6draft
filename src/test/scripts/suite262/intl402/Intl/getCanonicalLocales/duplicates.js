/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Duplicate language tags are omitted.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    ...
    7. Repeat, while k < len
      ...
      c. If kPresent is true, then
        ...
        vi. If canonicalizedTag is not an element of seen, append canonicalizedTag as the last element of seen.
        ...
---*/

var canonicalLocales = Intl.getCanonicalLocales(["wae-ch", "WAE-CH"]);

assert.sameValue(canonicalLocales.length, 1);
assert.sameValue(canonicalLocales[0], "wae-CH");
