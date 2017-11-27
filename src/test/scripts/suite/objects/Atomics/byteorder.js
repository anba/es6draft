/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

const isLittleEndian = new Int8Array(new Int32Array([1]).buffer)[0] === 1;

function bytes(typedArray) {
  let uint8 = new Uint8Array(typedArray.byteLength);
  uint8.set(new Uint8Array(typedArray.buffer));
  return uint8[isLittleEndian ? "reduceRight" : "reduce"]((a, v) => a + (v < 16 ? "0" : "") + v.toString(16), "");
}

function native(typedArray) {
  let uint8 = new Uint8Array(typedArray.byteLength);
  uint8.set(new Uint8Array(typedArray.buffer));
  return uint8.reduce((a, v) => a + (v < 16 ? "0" : "") + v.toString(16), "");
}

function switchEndian(typedArray) {
  // Implementation detail: Byte order mode is not reset in GetValueFromBuffer.
  // TODO: No longer applies, byte order isn't modified at runtime anymore.
  new DataView(typedArray.buffer).getInt32(0, !isLittleEndian);
}

// Store int16, first halfword
{
  let expected = bytes(new Int16Array([123, 0])); 
  let shared = new Int16Array(new SharedArrayBuffer(1 * 4));

  Atomics.store(shared, 0, 123);
  assertSame(expected, bytes(shared));

  switchEndian(shared);

  Atomics.store(shared, 0, 123);
  assertSame(expected, bytes(shared));
}

// Store int16, second halfword
{
  let expected = bytes(new Int16Array([0, 123])); 
  let shared = new Int16Array(new SharedArrayBuffer(1 * 4));

  Atomics.store(shared, 1, 123);
  assertSame(expected, bytes(shared));

  switchEndian(shared);

  Atomics.store(shared, 1, 123);
  assertSame(expected, bytes(shared));
}

// Store int32
{
  let expected = bytes(new Int32Array([123])); 
  let shared = new Int32Array(new SharedArrayBuffer(1 * 4));

  Atomics.store(shared, 0, 123);
  assertSame(expected, bytes(shared));

  switchEndian(shared);

  Atomics.store(shared, 0, 123);
  assertSame(expected, bytes(shared));
}

// Read int16, first halfword
{
  let expected = bytes(new Int16Array([123, 0])); 
  let shared = new Int16Array(new SharedArrayBuffer(1 * 4));
  shared[0] = 123;

  assertSame(123, Atomics.load(shared, 0));

  switchEndian(shared);

  assertSame(123, Atomics.load(shared, 0));
}

// Read int16, second halfword
{
  let expected = bytes(new Int16Array([0, 123])); 
  let shared = new Int16Array(new SharedArrayBuffer(1 * 4));
  shared[1] = 123;

  assertSame(123, Atomics.load(shared, 1));

  switchEndian(shared);

  assertSame(123, Atomics.load(shared, 1));
}

// Read int32
{
  let expected = bytes(new Int32Array([123])); 
  let shared = new Int32Array(new SharedArrayBuffer(1 * 4));
  shared[0] = 123;

  assertSame(123, Atomics.load(shared, 0));

  switchEndian(shared);

  assertSame(123, Atomics.load(shared, 0));
}
