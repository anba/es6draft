/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertThrows
} = Assert;

// B.3.3  Block-Level Function Declarations Web Legacy Compatibility Semantics
{
  const g = "fallback";

  function f1() {
    { function g() { return "g" } }
    assertSame("g", g());
  }
  f1();

  function f2() {
    { function g() { return "g1" } }
    { function g() { return "g2" } }
    assertSame("g2", g());
  }
  f2();

  function f3() {
    { function g() { return "g1" } }
    if (Math.random() < 0) { function g() { return "g2" } }
    assertSame("g1", g());
  }
  f3();

  function f4() {
    if (Math.random() < 0) { function g() { return "g" } }
    assertUndefined(g);
  }
  f4();

  function f5a() {
    { function g() {} }
    assertThrows(ReferenceError, () => g);
    return;
    const g = 0;
  }
  f5a();

  function f5b() {
    { function g() {} }
    assertThrows(ReferenceError, () => g);
    return;
    let g = 0;
  }
  f5b();

  function f6() {
    { function g() {} }
    let g = 0;
    assertSame(0, g);
  }
  f6();

  function f7() {
    let g = 0;
    { function g() {} }
    assertSame(0, g);
  }
  f7();

  function f8() {
    { function g() {} }
    const g = 0;
    assertSame(0, g);
  }
  f8();

  function f9() {
    const g = 0;
    { function g() {} }
    assertSame(0, g);
  }
  f9();

  function f10() {
    { function g() { return "block" } }
    function g() { return "top" }
    assertSame("block", g());
  }
  f10();

  function f11() {
    function g() { return "top" }
    { function g() { return "block" } }
    assertSame("block", g());
  }
  f11();

  function f12() {
    function g() { return "top" }
    assertSame("top", g());
    { function g() { return "block" } }
    assertSame("block", g());
  }
  f12();

  function f13() {
    assertSame("top", g());
    { function g() { return "block" } }
    assertSame("block", g());
    function g() { return "top" }
  }
  f13();

  function f14(g = 0) {
    assertSame(0, g);
    { function g() { return "g" } }
    assertSame(0, g);
  }
  f14();

  function f15(g = 0) {
    assertSame(1, g);
    { function g() { return "g" } }
    assertSame(1, g);
  }
  f15(1);

  function f16(g) {
    assertUndefined(g);
    { function g() { return "g" } }
    assertUndefined(g);
  }
  f16();

  function f17(g) {
    assertSame(1, g);
    { function g() { return "g" } }
    assertSame(1, g);
  }
  f17(1);
}