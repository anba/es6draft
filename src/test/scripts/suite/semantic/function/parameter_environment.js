/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertThrows,
  assertTrue,
  assertFalse,
  assertNotSame,
} = Assert;

// variable binding access tests
{
  let a = 99;

  function f1(f = () => a) {
    assertSame(99, f());
  }
  f1();

  function f2(a = 0, f = () => a) {
    assertSame(0, f());
  }
  f2();

  function f3(f = () => a, a = 0) {
    assertSame(0, f());
  }
  f3();

  function f4(f = () => a) {
    let a = 1;
    assertSame(99, f());
  }
  f4();

  function f5(f = () => a) {
    function a() { }
    assertSame(99, f());
  }
  f5();

  function f6(f = () => arguments) {
    assertSame(arguments, f());
  }
  f6();

  function f7(a = arguments) {
    assertSame(arguments, a);
  }
  f7();

  assertThrows(function f8(a = b, b = 0) { }, ReferenceError);

  assertThrows(function f9(a = b, b = 0) { "use strict" }, ReferenceError);

  assertThrows(function f10(a = (b = 1), b = 0) { }, ReferenceError);

  assertThrows(function f11(a = (b = 1), b = 0) { "use strict" }, ReferenceError);

  function f12(a = 0, f = () => a) {
    assertSame(0, f());
    a = 1;
    assertSame(1, a);
    assertSame(1, f());
  }
  f12();

  function f13(a, b = arguments[0]) {
    assertSame(1, a);
    assertSame(1, b);
  }
  f13(1);

  function f14() {
    function g1(arguments, h = () => arguments) {
      assertSame(void 0, h());
    }
    g1();

    function g2(arguments, h = () => arguments) {
      assertSame(0, h());
    }
    g2(0);

    function g3(h = () => arguments) {
      function arguments() { }
      assertSame(void 0, h());
    }
    g3();

    function g4(h = () => arguments) {
      let arguments;
      assertSame(void 0, h());
    }
    g4();

    function g5(h = () => arguments) {
      const arguments = 0;
      assertSame(void 0, h());
    }
    g5();

    function g6(h = () => arguments) {
      var arguments;
      assertSame(arguments, h());
      assertNotSame(void 0, arguments);
    }
    g6();
  }
  f14();
}

// functions with initializers, patterns and rest always get a strict-mode arguments object (parameter map)
{
  function defaultNoArgSetLocal(a = 1) {
    assertSame(1, a);
    assertSame(void 0, arguments[0]);
    a = 2;
    assertSame(2, a);
    assertSame(void 0, arguments[0]);
  }
  defaultNoArgSetLocal();

  function defaultArgSetLocal(a = 1) {
    assertSame(0, a);
    assertSame(0, arguments[0]);
    a = 2;
    assertSame(2, a);
    assertSame(0, arguments[0]);
  }
  defaultArgSetLocal(0);

  function defaultNoArgSetArguments(a = 1) {
    assertSame(1, a);
    assertSame(void 0, arguments[0]);
    arguments[0] = 2;
    assertSame(1, a);
    assertSame(2, arguments[0]);
  }
  defaultNoArgSetArguments();

  function defaultArgSetArguments(a = 1) {
    assertSame(0, a);
    assertSame(0, arguments[0]);
    arguments[0] = 2;
    assertSame(0, a);
    assertSame(2, arguments[0]);
  }
  defaultArgSetArguments(0);
}

// functions with initializers, patterns and rest always get a strict-mode arguments object (callee, caller)
{
  assertThrows(function(a = 0) { arguments.callee }, TypeError);
  assertThrows(function(a = 0) { arguments.caller }, TypeError);
  assertThrows(function([a]) { arguments.callee }, TypeError);
  assertThrows(function([a]) { arguments.caller }, TypeError);
  assertThrows(function({a}) { arguments.callee }, TypeError);
  assertThrows(function({a}) { arguments.caller }, TypeError);
  assertThrows(function(...a) { arguments.callee }, TypeError);
  assertThrows(function(...a) { arguments.caller }, TypeError);
}

// eval tests
{
  let a = 99; // outer binding

  function evalInBody(a = 0) {
    assertSame(0, a);
    eval("var a = 1");
    assertSame(1, a);
    assertTrue(delete a);
    assertSame(0, a);
    assertFalse(delete a);
    assertSame(0, a);
  }
  evalInBody();
  evalInBody(0);

  function evalInParams(_ = eval("var a = 0")) {
    assertSame(0, a);
    eval("var a = 1");
    assertSame(1, a);
    assertTrue(delete a);
    assertSame(0, a);
    assertTrue(delete a);
    assertSame(99, a);
    assertFalse(delete a);
    assertSame(99, a);
  }
  evalInParams();

  function evalInParamsLater(f = eval("var a = 0")) {
    assertSame(0, a);
    assertTrue(delete a);
    assertSame(99, a);
    eval("var a = 1");
    assertSame(1, a);
    assertTrue(delete a);
    assertSame(99, a);
    assertFalse(delete a);
    assertSame(99, a);
  }
  evalInParamsLater();

  function evalParamName(a = 0, b = eval("var a = 1")) {
    assertSame(1, a);
    assertFalse(delete a);
    assertSame(1, a);
  }
  evalParamName();

  function evalParamNameVar(_ = eval("var a"), a = 1) {
    assertSame(1, a);
    assertFalse(delete a);
    assertSame(1, a);
  }
  evalParamNameVar();

  assertThrows(function(_ = eval("var a = 0"), a = 1){ }, ReferenceError);

  function noAccessToDynamic(f = () => a) {
    assertSame(99, a);
    assertSame(99, f());
    eval("var a = 1");
    assertSame(1, a);
    assertSame(99, f());
    assertTrue(delete a);
    assertSame(99, a);
    assertSame(99, f());
    assertFalse(delete a);
    assertSame(99, a);
    assertSame(99, f());
  }
  noAccessToDynamic();
}
