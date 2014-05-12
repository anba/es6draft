/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse
} = Assert;

// 9.4.5.2 [[DefineOwnProperty]]: Throw if object is not initialized? 
// https://bugs.ecmascript.org/show_bug.cgi?id=2829

let indexedDesc = {value: 0, writable: true, enumerable: true, configurable: false};
assertFalse(Reflect.defineProperty(Int8Array[Symbol.create](), "0", indexedDesc));
assertFalse(Reflect.defineProperty(Int8Array[Symbol.create](), "1", indexedDesc));
