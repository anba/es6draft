/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertNull,
} = Assert;

// .caller and .legacy are restored after stack overflow
{
  function f() {
    f(); // stack overflow
  }
  function g() {
    try {
      f();
    } catch(e) {
      assertNull(f.caller);
      assertNull(f.arguments);
    }
  }
  g();
}

// .caller and .legacy are restored after stack overflow
{
  let arg = {};
  function exceedStack() {
    exceedStack();
  }
  function f(a0, callSelf) {
    if (callSelf) {
      f(a0);
      return;
    }
    try {
      exceedStack();
    } catch (e) {
      assertSame(f, f.caller);
      assertSame(1, f.arguments.length);
      assertSame(arg, f.arguments[0]);
    }
  }
  function g() {
    f(arg, true);
  }
  g();
}
