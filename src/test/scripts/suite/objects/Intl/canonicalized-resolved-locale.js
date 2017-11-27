/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

// Test ResolveLocale returns canonicalized Unicode locale extension sequences.

[void 0, {}, {localeMatcher: "lookup"}, {localeMatcher: "best fit"}].forEach(options => {
  var locale = "en-u-kn-false-kf-false";
  var supported = Intl.Collator.supportedLocalesOf(locale, options);
  assertSame(1, supported.length);
  assertSame("en-u-kf-false-kn-false", supported[0]);
  var collator = new Intl.Collator(locale, options);
  assertSame("en-u-kf-false-kn-false", collator.resolvedOptions().locale);
});
