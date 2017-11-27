/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 19.1.1.1 Object: Change ObjectCreate to OrdinaryCreateFromConstructor?
// https://bugs.ecmascript.org/show_bug.cgi?id=3136

function Obj() {}
Obj.prototype = {};

assertSame(Obj.prototype, Object.getPrototypeOf(Reflect.construct(Object, [], Obj)));
assertSame(Obj.prototype, Object.getPrototypeOf(Reflect.construct(Object, [void 0], Obj)));
assertSame(Obj.prototype, Object.getPrototypeOf(Reflect.construct(Object, [null], Obj)));
