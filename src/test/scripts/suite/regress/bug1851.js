/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 8.3.16.10: Invalid assertion in step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=1851
// https://bugs.ecmascript.org/show_bug.cgi?id=1613

// let f = new class extends Function { constructor() { /* no super */ } };
// Object.defineProperty(f, "prototype", {get() { return String.prototype }});
// assertThrows(TypeError, () => Function.call(f, "return 1"));
// assertSame(1, f());
// assertSame(String.prototype, Object.getPrototypeOf(new f));

assertThrows(ReferenceError, () => new class extends Function { constructor() { /* no super */ } });
