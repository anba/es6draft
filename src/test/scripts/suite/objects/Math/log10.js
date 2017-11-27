/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction,
  assertSame,
} = Assert;


/* 20.2.2.21  Math.log10 ( x ) */

assertBuiltinFunction(Math.log10, "log10", 1);

assertSame(0 / 0, Math.log10(0 / 0));
assertSame(0 / 0, Math.log10(-1 / 0));
assertSame(+1 / 0, Math.log10(+1 / 0));
assertSame(-1 / 0, Math.log10(-0));
assertSame(-1 / 0, Math.log10(+0));
assertSame(+0, Math.log10(1));

for (var k = 0; k <= 22; ++k) {
  assertSame(k, Math.log10(Math.pow(10, k)));
}
