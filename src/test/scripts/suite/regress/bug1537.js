/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 8.3.15.6: FunctionInitialize should set internal properties after user-modifiable properties
// https://bugs.ecmascript.org/show_bug.cgi?id=1537

let fn = Function[Symbol.create]();
Object.defineProperty(fn, "length", {value: -1});
assertThrows(TypeError, () => Function.call(fn, ""));
assertThrows(TypeError, fn);
