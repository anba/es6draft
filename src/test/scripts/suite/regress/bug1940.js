/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 22.2.1.3: Missing [[ViewedArrayBuffer]] state check in %TypedArray% constructor
// https://bugs.ecmascript.org/show_bug.cgi?id=1940

const TypedArray = Object.getPrototypeOf(Int8Array);

{
  let ta = new class extends Int8Array { constructor() { /* no super */ } };
  assertThrows(TypeError, () => Int8Array.call(ta, {get length() {TypedArray.call(ta, 0); return 10}}));
  assertSame(0, ta.length);
}
