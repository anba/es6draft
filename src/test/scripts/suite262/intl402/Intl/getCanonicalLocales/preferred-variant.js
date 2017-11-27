/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Call Intl.getCanonicalLocales function with grandfathered language tags.
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
        v. Let canonicalizedTag be CanonicalizeLanguageTag(tag).
        vi. If canonicalizedTag is not an element of seen, append canonicalizedTag as the last element of seen.
      ...
includes: [testIntl.js]
---*/

// Generated from http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
// File-Date: 2017-08-15
var canonicalizedTags = {
  "ja-latn-hepburn-heploc": "ja-Latn-alalc97",
};

// make sure the data above is correct
Object.getOwnPropertyNames(canonicalizedTags).forEach(function (tag) {
  var canonicalizedTag = canonicalizedTags[tag];
  assert(
    isCanonicalizedStructurallyValidLanguageTag(canonicalizedTag),
    "Test data \"" + canonicalizedTag + "\" is not canonicalized and structurally valid language tag."
  );
});

Object.getOwnPropertyNames(canonicalizedTags).forEach(function (tag) {
  var canonicalLocales = Intl.getCanonicalLocales(tag);
  assert.sameValue(canonicalLocales.length, 1);
  assert.sameValue(canonicalLocales[0], canonicalizedTags[tag]);
});
