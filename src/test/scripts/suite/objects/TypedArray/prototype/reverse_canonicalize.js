/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue
} = Assert;

const isLittleEndian = new Uint8Array(new Uint16Array([1]).buffer)[0] === 1;

function toRawBits(buffer, offset, length) {
  return new Uint8Array(buffer, offset, length)[isLittleEndian ? 'reduceRight' : 'reduce'](
    (acc, value) => acc + (value < 16 ? "0" : "") + value.toString(16), ""
  );
}

for (let C of [Float32Array, Float64Array]) {
  let array = new C([NaN]);
  let canon = toRawBits(new C([NaN]).buffer, 0, C.BYTES_PER_ELEMENT);

  // Add NaN payload.
  new Uint8Array(array.buffer)[isLittleEndian ? 0 : (C.BYTES_PER_ELEMENT - 1)] = 0x01;

  let withPayload = toRawBits(array.buffer, 0, C.BYTES_PER_ELEMENT);
  assertTrue(withPayload.endsWith("1"), `withPayload = ${withPayload}`);
  assertNotSame(canon, withPayload);

  array.reverse();

  assertSame(withPayload, toRawBits(array.buffer, 0, C.BYTES_PER_ELEMENT));
}

for (let C of [Float32Array, Float64Array]) {
  let array = new C([NaN, 0]);
  let canon = toRawBits(new C([NaN]).buffer, 0, C.BYTES_PER_ELEMENT);

  // Add NaN payload.
  new Uint8Array(array.buffer)[isLittleEndian ? 0 : (C.BYTES_PER_ELEMENT - 1)] = 0x01;

  let withPayload = toRawBits(array.buffer, 0, C.BYTES_PER_ELEMENT);
  assertTrue(withPayload.endsWith("1"), `withPayload = ${withPayload}`);
  assertNotSame(canon, withPayload);

  array.reverse();

  assertSame(canon, toRawBits(array.buffer, C.BYTES_PER_ELEMENT, C.BYTES_PER_ELEMENT));
}
