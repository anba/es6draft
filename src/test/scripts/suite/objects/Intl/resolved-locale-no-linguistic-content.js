/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Test lookup and best fit matcher with "no linguistic content" locale

for (let c of [Intl.Collator, Intl.DateTimeFormat, Intl.NumberFormat]) {
  for (let matcher of ["lookup", "best fit"]) {
    let locale = "zxx"; // "no linguistic content" locale
    let supported = c.supportedLocalesOf(locale, {localeMatcher: matcher});
    assertSame(0, supported.length);
    let defaultLocale = new c().resolvedOptions().locale;
    let resolvedLocale = new c(locale, {localeMatcher: matcher}).resolvedOptions().locale;
    assertSame(defaultLocale, resolvedLocale);
  }
}
