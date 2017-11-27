/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.13 Function Declaration Instantiation: Visibility of function declaration with name "arguments"?
// https://bugs.ecmascript.org/show_bug.cgi?id=2961

function f1(g = arguments) {
  function arguments() { }
  assertSame("object", typeof g);
  assertSame("function", typeof arguments);
}
f1();

function f2(g = () => arguments) {
  function arguments() { }
  assertSame("object", typeof g());
  assertSame("function", typeof arguments);
}
f2();
