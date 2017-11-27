/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.2.4.1 TypedArray: Missing type check IsConstructor
// https://bugs.ecmascript.org/show_bug.cgi?id=3655

for (let superCtor of [null, {}, () => {}]) {
  class TypedArray extends Int8Array { }
  Object.setPrototypeOf(TypedArray, superCtor);
  assertThrows(TypeError, () => new TypedArray);
}
