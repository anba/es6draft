/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Implementations are allowed to canonicalize extension subtag sequences.
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
        ...

  6.2.3 CanonicalizeLanguageTag (locale)
    The specifications for extensions to BCP 47 language tags, such as
    RFC 6067, may include canonicalization rules for the extension subtag
    sequences they define that go beyond the canonicalization rules of
    RFC 5646 section 4.5. Implementations are allowed, but not required,
    to apply these additional rules.
---*/

var locale = "it-u-nu-latn-ca-gregory";

// RFC 6067: The canonical order of keywords is in US-ASCII order by key.
var sorted = "it-u-ca-gregory-nu-latn";

var canonicalLocales = Intl.getCanonicalLocales(locale);
assert.sameValue(canonicalLocales.length, 1);

var canonicalLocale = canonicalLocales[0];
assert((canonicalLocale === locale) || (canonicalLocale === sorted));
