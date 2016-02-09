/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.1.3.31 Array.prototype [ @@unscopables ]: Don't let blackList inherit from ObjectPrototype
// https://bugs.ecmascript.org/show_bug.cgi?id=3196

var v;
with([]) v = toString;
assertSame(Array.prototype.toString, v);
