/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.14 Function Declaration Instantiation: Bindings for function declarations not created
// https://bugs.ecmascript.org/show_bug.cgi?id=2643

function f(a) {
  return typeof a;
  function a(){}
}
assertSame("function", f(0));

function g(a) {
  return typeof arguments;
  function arguments(){}
}
assertSame("function", g(0));
