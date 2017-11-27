/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Update: Number('0b0101')
// https://bugs.ecmascript.org/show_bug.cgi?id=3259

assertSame(0b0101, Number('0b0101'));
assertSame(0, parseInt('0b0101'));

assertSame(NaN, Number('0b0102'));
assertSame(0, parseInt('0b0102'));

assertSame(0o0107, Number('0o0107'));
assertSame(0, parseInt('0o0107'));

assertSame(NaN, Number('0b0108'));
assertSame(0, parseInt('0b0108'));