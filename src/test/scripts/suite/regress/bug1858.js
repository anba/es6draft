/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// More ToInteger() / ToUint32() -> ToLength() changes
// https://bugs.ecmascript.org/show_bug.cgi?id=1858

assertSame(0, (function(){ return arguments.length }).apply(null, {length: -0xffffffff}));
assertSame(0, "".split(void 0, -0xffffffff).length);
assertSame(0, Array.from({length: -0xffffffff}).length);
assertSame(0, Int8Array.from({length: -0xffffffff}).length);
