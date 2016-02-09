/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 8.3.15.1: [[Call]] behaviour for uninitialised function objects not defined
// https://bugs.ecmascript.org/show_bug.cgi?id=1535

// assertThrows(TypeError, () => (new class extends Function { constructor(){} })());
// assertThrows(TypeError, () => Function[Symbol.create]()());

assertThrows(ReferenceError, () => (new class extends Function { constructor(){} })());
