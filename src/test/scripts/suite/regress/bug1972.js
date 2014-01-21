/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 26.1.8, Reflect.hasOwn(): unusual evaluation order
// https://bugs.ecmascript.org/show_bug.cgi?id=1972

class MyError extends Error {}

assertThrows(() => Reflect.hasOwn(void 0, {toString(){ throw new MyError }}), TypeError);
assertThrows(() => Reflect.hasOwn(null, {toString(){ throw new MyError }}), TypeError);
assertThrows(() => Reflect.hasOwn("", {toString(){ throw new MyError }}), MyError);
