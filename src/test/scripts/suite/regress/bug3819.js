/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.4.1.3 BoundFunctionCreate should use target function's [[Prototype]]
// https://bugs.ecmascript.org/show_bug.cgi?id=3819

function F() {}
Object.setPrototypeOf(F, Object.prototype);

var boundF = Function.prototype.bind.call(F);

assertSame(Object.prototype, Object.getPrototypeOf(boundF));
