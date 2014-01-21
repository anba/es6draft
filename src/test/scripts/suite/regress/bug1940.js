/*
 * Copyright (c) 2012-2014 André Bargull
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
  let ta = Int8Array[Symbol.create]();
  assertThrows(() => Int8Array.call(ta, {get length() {TypedArray.call(ta, 0); return 10}}), TypeError);
  assertSame(0, ta.length);
}
