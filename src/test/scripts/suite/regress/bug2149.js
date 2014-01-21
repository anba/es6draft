/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 22.2.1: .name property for %TypedArray%
// https://bugs.ecmascript.org/show_bug.cgi?id=2149

assertSame("TypedArray", Object.getPrototypeOf(Int8Array).name);
