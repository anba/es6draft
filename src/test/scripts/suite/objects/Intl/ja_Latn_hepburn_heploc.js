/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

// Test ja-Latn-hepburn-heploc is resolved to ja-Latn-alalc97

[Intl.Collator, Intl.DateTimeFormat, Intl.NumberFormat].forEach(C => {
  [void 0, {}, {localeMatcher: "lookup"}, {localeMatcher: "best fit"}].forEach(options => {
    var supported = C.supportedLocalesOf("ja-Latn-hepburn-heploc", options);
    assertSame(1, supported.length);
    assertSame("ja-Latn-alalc97", supported[0]);
  });
});
