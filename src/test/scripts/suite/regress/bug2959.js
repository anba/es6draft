/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// B.3.2 Web Legacy Block Function: Handle duplicate block function declarations
// https://bugs.ecmascript.org/show_bug.cgi?id=2959

function f1() {
  { function g(){ return 1 } }
  { function g(){ return 2 } }
  assertSame(2, g());
}
f1();

function f2() {
  { function g(){ return 1 } }
  assertSame(1, g());
  { function g(){ return 2 } }
  assertSame(2, g());
}
f2();

function f3() {
  assertUndefined(g);
  { function g(){ return 1 } }
  assertSame(1, g());
  { function g(){ return 2 } }
  assertSame(2, g());
}
f3();

function f4() {
  assertUndefined(g);
  return;
  { function g(){ return 1 } }
  { function g(){ return 2 } }
}
f4();

function f5() {
  assertUndefined(g);
  if (Math.random() >= 0) return;
  { function g(){ return 1 } }
  { function g(){ return 2 } }
}
f5();
