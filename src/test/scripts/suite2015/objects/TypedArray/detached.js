/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals,
} = Assert;

// MOP access on detached typed arrays

function create(length = 0) {
  let array = new Int8Array(length);
  detachArrayBuffer(array.buffer);
  return array;
}

// [[Enumerate]]
assertEquals([], [...Reflect.enumerate(create())]);
assertEquals(["0"], [...Reflect.enumerate(create(1))]);
