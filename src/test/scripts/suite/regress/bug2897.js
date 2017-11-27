/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty, assertTrue
} = Assert;

// 9.4.5.2 [[DefineOwnProperty]]: Intentional to prohibit all property definitions on uninitialized integer indexed objects?
// https://bugs.ecmascript.org/show_bug.cgi?id=2897

// let ta = new class extends Int8Array { constructor() { /* no super */ } };

// assertTrue(Reflect.defineProperty(ta, "abc", {value: 0}));
// assertTrue(Reflect.defineProperty(ta, "def", {value: 1, writable: true, configurable: true}));

// assertDataProperty(ta, "abc", {value: 0, writable: false, enumerable: false, configurable: false});
// assertDataProperty(ta, "def", {value: 1, writable: true, enumerable: false, configurable: true});
