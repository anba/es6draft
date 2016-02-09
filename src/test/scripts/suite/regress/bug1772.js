/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.7.3.9, 15.7.3.10: Define "Same as 15.1.2.x"
// https://bugs.ecmascript.org/show_bug.cgi?id=1772

assertSame(parseFloat, Number.parseFloat);
assertSame(parseInt, Number.parseInt);
