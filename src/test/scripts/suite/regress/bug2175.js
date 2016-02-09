/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

//  22.2.1.2 %TypedArray%: Different [[Prototype]] for copied ArrayBuffer depending on element types
// https://bugs.ecmascript.org/show_bug.cgi?id=2175

class MyArrayBuffer extends ArrayBuffer {}
let source = new Int8Array(new MyArrayBuffer(10));

let copySameType = new Int8Array(source);
assertSame(copySameType.buffer.constructor, MyArrayBuffer);
assertSame(Object.getPrototypeOf(copySameType.buffer), MyArrayBuffer.prototype);

let copyDifferentType = new Uint8Array(source);
assertSame(copyDifferentType.buffer.constructor, MyArrayBuffer);
assertSame(Object.getPrototypeOf(copyDifferentType.buffer), MyArrayBuffer.prototype);
