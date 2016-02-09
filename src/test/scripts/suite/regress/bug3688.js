/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 18.2.4 parseFloat, 18.2.5 parseInt: Handle zero before applying "Number value for"
// https://bugs.ecmascript.org/show_bug.cgi?id=3688

assertSame(0, parseInt("0"));
assertSame(0, parseInt("+0"));
assertSame(-0, parseInt("-0"));

assertSame(0, parseFloat("0"));
assertSame(0, parseFloat("+0"));
assertSame(-0, parseFloat("-0"));
