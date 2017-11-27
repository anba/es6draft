/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

class Fn extends Function {
  constructor(...args) {
    super(...args);
  }
}
var f = new Fn("return Object");
var r = new Reflect.Realm();
var result = r.global.Function.prototype.call.call(f);

assertSame(Object, result);
