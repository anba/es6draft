/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

// Test supportedLocalesOf

for (let c of [Intl.Collator, Intl.DateTimeFormat, Intl.NumberFormat]) {
  for (let o of ["de", ["de"], [new String("de")]]) {
    let supported = c.supportedLocalesOf(o);
    assertSame(1, supported.length);
    assertSame("de", supported[0]);
  }
}
