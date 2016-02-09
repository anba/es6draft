/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

// BestFitMatcher+ResolveLocale may produce invalid language tags
// https://bugs.ecmascript.org/show_bug.cgi?id=1456


function testLocale(C, locale, unicodeExtension) {
  var localeOptions = {localeMatcher: "best fit"};
  var resolvedLocale = new C(locale + unicodeExtension, localeOptions).resolvedOptions().locale;
  assertTrue(resolvedLocale.endsWith(unicodeExtension),
    `locale=${locale}, unicodeExtension=${unicodeExtension}, resolvedLocale=${resolvedLocale}`);
}

testLocale(Intl.Collator, "ms-bn", "-u-kf-false");
testLocale(Intl.Collator, "sr-xk", "-u-kf-false");

testLocale(Intl.DateTimeFormat, "ms-bn", "-u-ca-gregory");
testLocale(Intl.DateTimeFormat, "sr-xk", "-u-ca-gregory");

testLocale(Intl.NumberFormat, "ms-bn", "-u-nu-latn");
testLocale(Intl.NumberFormat, "sr-xk", "-u-nu-latn");
