/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

// Test en-US-POSIX is not resolved to en-US-u-va-posix

[Intl.Collator, Intl.DateTimeFormat, Intl.NumberFormat].forEach(C => {
  [void 0, {}, {localeMatcher: "lookup"}, {localeMatcher: "best fit"}].forEach(options => {
    var supported = C.supportedLocalesOf("en-US-POSIX", options);
    assertSame(1, supported.length);
    assertSame("en-US-posix", supported[0]);
    assertSame("en-US", new C("en-US-POSIX", options).resolvedOptions().locale);
  });
});
