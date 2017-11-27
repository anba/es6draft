/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertUndefined
} = Assert;

// 26.1.8, Reflect.hasOwn(): unusual evaluation order
// https://bugs.ecmascript.org/show_bug.cgi?id=1972

// class MyError extends Error {}
// 
// assertThrows(TypeError, () => Reflect.hasOwn(void 0, {toString(){ throw new MyError }}));
// assertThrows(TypeError, () => Reflect.hasOwn(null, {toString(){ throw new MyError }}));
// assertThrows(MyError, () => Reflect.hasOwn("", {toString(){ throw new MyError }}));

assertUndefined(Reflect.hasOwn);
