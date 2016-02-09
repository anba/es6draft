/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// Improve TypedArray constructor/Proxy interaction
// https://github.com/tc39/ecma262/pull/247

const TypedArray = Object.getPrototypeOf(Int8Array);

assertThrows(TypeError, () => TypedArray());
assertThrows(TypeError, () => new TypedArray());

// Ensure "getPrototypeOf" trap is not called
Reflect.construct(Int8Array, [], new Proxy(TypedArray, {
  getPrototypeOf() {
    fail `getPrototypeOf trap called`;
  }
}));
