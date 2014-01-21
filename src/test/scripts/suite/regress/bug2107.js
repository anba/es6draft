/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 9.3 Built-in Function Objects: Clarify whether built-in functions can change their [[Prototype]]
// https://bugs.ecmascript.org/show_bug.cgi?id=2107

assertSame(Function.prototype, Object.getPrototypeOf(Object.create));

Object.setPrototypeOf(Object.create, Object.prototype);
assertSame(Object.prototype, Object.getPrototypeOf(Object.create));
