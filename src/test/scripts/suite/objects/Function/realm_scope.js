/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

class Fn extends Function {
  constructor() {}
}
var f = new Fn();
var r = new Reflect.Realm();
var g = r.global.Function.call(f, "return Object");

assertSame(f, g);
assertSame(Object, g());
