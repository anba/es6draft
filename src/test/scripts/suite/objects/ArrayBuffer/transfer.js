/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Detached array buffer.
{
  let buf = new ArrayBuffer(1);
  detachArrayBuffer(buf);
  assertThrows(TypeError, () => ArrayBuffer.transfer(buf));
  assertThrows(TypeError, () => ArrayBuffer.transfer(buf, 0));
  assertThrows(TypeError, () => ArrayBuffer.transfer(buf, 1));
}

// Transfer without newByteLength parameter.
for (let len of [0, 1, 2, 3, 4, 100, 1024]) {
  let src = new ArrayBuffer(len);
  assertSame(len, src.byteLength);
  let dest = ArrayBuffer.transfer(src);
  assertSame(len, dest.byteLength);
  assertThrows(TypeError, () => src.byteLength);
}

// Transfer with newByteLength parameter.
for (let len of [0, 1, 2, 3, 4, 100, 1024]) {
  for (let delta of [-1000, -2, -1, 0, 1, 2, 1000]) {
    let newLen = len + delta;
    if (newLen < 0) continue;
    let src = new ArrayBuffer(len);
    assertSame(len, src.byteLength);
    let dest = ArrayBuffer.transfer(src, newLen);
    assertSame(newLen, dest.byteLength);
    assertThrows(TypeError, () => src.byteLength);
  }
}
