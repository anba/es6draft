/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue, assertFalse, assertUndefined
} = Assert;

// toMethod should create a function without a prototype property
// https://bugs.ecmascript.org/show_bug.cgi?id=3392

function F(){}
assertTrue(F.hasOwnProperty("prototype"));
assertNotSame(Object.prototype, F.prototype);

var clone = F.toMethod({});
assertFalse(clone.hasOwnProperty("prototype"));
assertUndefined(clone.prototype);

Object.prototype.prototype = 123;
assertSame(123, clone.prototype);
