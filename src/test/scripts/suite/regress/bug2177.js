/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

//  24.1.1.3 CloneArrayBuffer: Invalid assertion in step 7
// https://bugs.ecmascript.org/show_bug.cgi?id=2177

let source = new Int8Array(new ArrayBuffer(0));
let target = new Int8Array(source);

assertSame(source.length, target.length);
assertSame(source.byteLength, target.byteLength);
