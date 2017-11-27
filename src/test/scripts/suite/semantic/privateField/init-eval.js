/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

function f() {
  var x = 123;
  class C {
    #private = eval("x");
    get() { return this.#private; }
  }
  return new C();
}
let o = f();
assertSame(123, o.get());


function g() {
  class C {
    #private = eval("C");
    get() { return this.#private; }
  }
  return new C();
}
let o2 = g();
assertSame(o2.constructor, o2.get());
