/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 19.1.1.1 Object: Change ObjectCreate to OrdinaryCreateFromConstructor?
// https://bugs.ecmascript.org/show_bug.cgi?id=3136

var Obj = Object.toMethod({});
Obj.prototype = {};

// FIXME: Update test after bug 3550 is fixed

assertSame(Object.prototype, Object.getPrototypeOf(Obj()));
assertSame(Object.prototype, Object.getPrototypeOf(Obj(void 0)));
assertSame(Object.prototype, Object.getPrototypeOf(Obj(null)));

assertSame(Object.prototype, Object.getPrototypeOf(new Obj()));
assertSame(Object.prototype, Object.getPrototypeOf(new Obj(void 0)));
assertSame(Object.prototype, Object.getPrototypeOf(new Obj(null)));
