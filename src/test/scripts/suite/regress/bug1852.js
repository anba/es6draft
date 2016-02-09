/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 8.3.16.6: Missing ReturnIfAbrupt after step 2
// https://bugs.ecmascript.org/show_bug.cgi?id=1852

// let fn = new class extends Function { constructor() { /* no super */ } };
// Object.defineProperty(fn, "length", {value: null});
// assertThrows(TypeError, () => Function.call(fn, ""));

assertThrows(ReferenceError, () => new class extends Function { constructor() { /* no super */ } });
