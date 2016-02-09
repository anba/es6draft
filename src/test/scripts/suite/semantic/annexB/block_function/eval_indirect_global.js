/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertDataProperty, assertTrue, assertFalse, assertUndefined
} = Assert;

// B.3.3  Block-Level Function Declarations Web Legacy Compatibility Semantics
// ES2016: EvalDeclarationInstantiation on global

// Basic test: Check block scoped works in non-strict scripts.
(0, eval)(`{ function f0() {} }`);
assertSame("function", typeof f0);
assertTrue("f0" in this);
assertDataProperty(this, "f0", {value: f0, writable: true, enumerable: true, configurable: true});

// Basic test: Check block scoped not applied in strict scripts.
(0, eval)(`"use strict"; { function g0() {} }`);
assertSame("undefined", typeof g0);
assertFalse("g0" in this);

// Legacy semantics not used if global lexical found.
let f1 = 0;
(0, eval)(`{ function f1() {} }`);
assertSame("number", typeof f1);
assertFalse("f1" in this);

// Legacy semantics used if existing property is configurable. (1)
Object.defineProperty(this, "f2", {value: 0, writable: false, enumerable: false, configurable: true});
(0, eval)(`{ function f2() {} }`);
assertSame("function", typeof f2);

// Legacy semantics used if existing property is configurable. (2)
Object.defineProperty(this, "f3", {get(){}, enumerable: false, configurable: true});
(0, eval)(`{ function f3() {} }`);
assertSame("function", typeof f3);

// Legacy semantics used if existing data-property is non-configurable, but writable and enumerable.
Object.defineProperty(this, "f4", {value: 0, writable: true, enumerable: true, configurable: false});
(0, eval)(`{ function f4() {} }`);
assertSame("function", typeof f4);

// Legacy semantics not used if existing data-property is non-configurable, non-writable and enumerable.
Object.defineProperty(this, "f5", {value: 0, writable: false, enumerable: true, configurable: false});
(0, eval)(`{ function f5() {} }`);
assertSame("number", typeof f5);

// Legacy semantics not used if existing data-property is non-configurable, writable and non-enumerable.
Object.defineProperty(this, "f6", {value: 0, writable: true, enumerable: false, configurable: false});
(0, eval)(`{ function f6() {} }`);
assertSame("number", typeof f6);

// Legacy semantics not used if existing data-property is non-configurable, non-writable and non-enumerable.
Object.defineProperty(this, "f7", {value: 0, writable: false, enumerable: false, configurable: false});
(0, eval)(`{ function f7() {} }`);
assertSame("number", typeof f7);

// Check creation property order.
(0, eval)(`function f8_1(){} var f8_2; { function f8_3(){} } { function f8_4(){} } var f8_5; function f8_6(){}`);
assertEquals([3, 4, 1, 6, 2, 5], Object.getOwnPropertyNames(this).filter(n => n.startsWith("f8_")).map(n => +n.replace("f8_", "")));

// Global function binding is initialized with "undefined".
(0, eval)(`
  assertUndefined(f9);
  assertUndefined(this.f9);
  {
    assertSame("function", typeof f9);
    assertUndefined(this.f9);
    function f9(){}
    assertSame("function", typeof f9);
    assertSame("function", typeof this.f9);
  }
  assertSame("function", typeof f9);
  assertSame("function", typeof this.f9);
`);

// Check function assignment with setter.
(0, eval)(`{
  let setterCalled = false;
  Object.defineProperty(this, "f10", {
    set(f) {
      assertFalse(setterCalled);
      setterCalled = true;
      assertSame("function", typeof f);
    },
    configurable: false
  });
  assertFalse(setterCalled);
  function f10() {}
  assertTrue(setterCalled);
}`);

// Legacy semantics used in nested eval calls.
(0, eval)(`
  assertSame("undefined", typeof f11);
  assertFalse("f11" in this);
  eval(\`
    assertUndefined(f11);
    assertUndefined(this.f11);
    { function f11() {} }
    assertSame("function", typeof f11);
    assertSame("function", typeof this.f11);
  \`);
  assertSame("function", typeof f11);
  assertTrue("f11" in this);
`);

// Legacy semantics not used if lexical found.
(0, eval)(`
  let f12 = 0;
  eval(\`
    { function f12() {} }
    assertSame("number", typeof f12);
  \`);
`);
