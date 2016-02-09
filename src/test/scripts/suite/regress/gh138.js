/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Date.parse treats date-only no offset as UTC
// https://github.com/tc39/ecma262/pull/138

assertSame(1446076800000, Date.parse("2015-10-29"));
assertSame(1446102000000, Date.parse("2015-10-29T00:00"));
assertSame(1446102000000, Date.parse("2015-10-29T00:00-07:00"));
assertSame(1446076800000, Date.parse("2015-10-29T00:00Z"));
