/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertSame,
} = Assert;


/* 20.2.2.23  Math.log2 ( x ) */

assertBuiltinFunction(Math.log2, "log2", 1);

assertSame(0 / 0, Math.log2(0 / 0));
assertSame(0 / 0, Math.log2(-1 / 0));
assertSame(+1 / 0, Math.log2(+1 / 0));
assertSame(-1 / 0, Math.log2(-0));
assertSame(-1 / 0, Math.log2(+0));
assertSame(+0, Math.log2(1));

for (var k = -1074; k <= 1023; ++k) {
  assertSame(k, Math.log2(Math.pow(2, k)));
}
