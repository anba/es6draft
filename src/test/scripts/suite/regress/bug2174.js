/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.2.1.2 %TypedArray%: Check [[ViewedArrayBuffer]] state before assignment
// https://bugs.ecmascript.org/show_bug.cgi?id=2174

var buf = new ArrayBuffer(1);
Object.defineProperty(buf, "constructor", {
  get() {
    Object.getPrototypeOf(Int8Array).call(t, 1);
    return ArrayBuffer;
  }
});
var s = new Int8Array(buf);
// var t = new class extends Int8Array { constructor() { /* no super */ } };
// assertThrows(TypeError, () => Int8Array.call(t, s));
assertThrows(ReferenceError, () => new class extends Int8Array { constructor() { /* no super */ } });
