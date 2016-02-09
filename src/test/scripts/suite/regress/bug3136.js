/*
 * Copyright (c) 2012-2016 André Bargull
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

assertSame(Obj.prototype, Object.getPrototypeOf(Obj()));
assertSame(Obj.prototype, Object.getPrototypeOf(Obj(void 0)));
assertSame(Obj.prototype, Object.getPrototypeOf(Obj(null)));

assertSame(Obj.prototype, Object.getPrototypeOf(new Obj()));
assertSame(Obj.prototype, Object.getPrototypeOf(new Obj(void 0)));
assertSame(Obj.prototype, Object.getPrototypeOf(new Obj(null)));
