/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.13.7.2.1: Zero length DataViews not possible
// https://bugs.ecmascript.org/show_bug.cgi?id=1680

assertSame(0, new DataView(new ArrayBuffer(1), 1).byteLength);
assertSame(0, new DataView(new ArrayBuffer(1), 1, 0).byteLength);
