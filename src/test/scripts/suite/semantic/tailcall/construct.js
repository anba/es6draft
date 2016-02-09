/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

const {
  assertSame, assertThrows, assertTrue, assertFalse
} = Assert;

{
  function k() { return 0; }
  function h() { return k(); }
  function g() { return h(); }
  function f() { return new g(); }

  let o = new f();
  assertSame(g.prototype, Object.getPrototypeOf(o));
}

{
  let fCalled = false;
  function f() {
    assertFalse(fCalled);
    fCalled = true;
    return 0;
  }
  class Base { }
  class Derived extends Base {
    constructor() {
      super();
      return f();
    }
  }
  assertFalse(fCalled);
  assertThrows(TypeError, () => new Derived());
  assertTrue(fCalled);
}
