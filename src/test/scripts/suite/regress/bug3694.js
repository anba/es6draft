/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue
} = Assert;

// 22.2.3.22.2%TypedArray%.prototype.set: Special case same element to allow memmove?
// https://bugs.ecmascript.org/show_bug.cgi?id=3694

const isLittleEndian = new Uint8Array(new Uint16Array([1]).buffer)[0] === 1;

function toRawBits(buffer, offset, length) {
  return new Uint8Array(buffer, offset, length)[isLittleEndian ? 'reduceRight' : 'reduce'](
    (acc, value) => acc + (value < 16 ? "0" : "") + value.toString(16), ""
  );
}

for (let C of [Float32Array, Float64Array]) {
  let floatSource = new C([NaN]);
  let floatTarget = new C(1);
  let canon = toRawBits(new C([NaN]).buffer, 0, C.BYTES_PER_ELEMENT);

  // Add NaN payload.
  new Uint8Array(floatSource.buffer)[isLittleEndian ? 0 : (C.BYTES_PER_ELEMENT - 1)] = 0x01;

  let withPayload = toRawBits(floatSource.buffer, 0, C.BYTES_PER_ELEMENT);
  assertTrue(withPayload.endsWith("1"), `withPayload = ${withPayload}`);
  assertNotSame(canon, withPayload);

  floatTarget.set(floatSource, 0);

  assertSame(withPayload, toRawBits(floatTarget.buffer, 0, C.BYTES_PER_ELEMENT));
}
