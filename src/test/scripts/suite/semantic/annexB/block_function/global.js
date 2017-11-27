/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertDataProperty, assertTrue, assertFalse, assertUndefined
} = Assert;

// B.3.3  Block-Level Function Declarations Web Legacy Compatibility Semantics
// ES2016: GlobalDeclarationInstantiation

// Basic test: Check block scoped works in non-strict scripts.
evalScript(`{ function f0() {} }`);
assertSame("function", typeof f0);
assertTrue("f0" in this);
assertDataProperty(this, "f0", {value: f0, writable: true, enumerable: true, configurable: false});

// Basic test: Check block scoped not applied in strict scripts.
evalScript(`"use strict"; { function g0() {} }`);
assertSame("undefined", typeof g0);
assertFalse("g0" in this);

// Legacy semantics not used if global lexical found.
let f1 = 0;
evalScript(`{ function f1() {} }`);
assertSame("number", typeof f1);
assertFalse("f1" in this);

// Legacy semantics used if existing property is configurable. (1)
Object.defineProperty(this, "f2", {value: 0, writable: false, enumerable: false, configurable: true});
evalScript(`{ function f2() {} }`);
assertSame("function", typeof f2);

// Legacy semantics used if existing property is configurable. (2)
Object.defineProperty(this, "f3", {get(){}, enumerable: false, configurable: true});
evalScript(`{ function f3() {} }`);
assertSame("function", typeof f3);

// Legacy semantics used if existing data-property is non-configurable, but writable and enumerable.
Object.defineProperty(this, "f4", {value: 0, writable: true, enumerable: true, configurable: false});
evalScript(`{ function f4() {} }`);
assertSame("function", typeof f4);

// Legacy semantics not used if existing data-property is non-configurable, non-writable and enumerable.
Object.defineProperty(this, "f5", {value: 0, writable: false, enumerable: true, configurable: false});
evalScript(`{ function f5() {} }`);
assertSame("number", typeof f5);

// Legacy semantics not used if existing data-property is non-configurable, writable and non-enumerable.
Object.defineProperty(this, "f6", {value: 0, writable: true, enumerable: false, configurable: false});
evalScript(`{ function f6() {} }`);
assertSame("number", typeof f6);

// Legacy semantics not used if existing data-property is non-configurable, non-writable and non-enumerable.
Object.defineProperty(this, "f7", {value: 0, writable: false, enumerable: false, configurable: false});
evalScript(`{ function f7() {} }`);
assertSame("number", typeof f7);

// Check creation property order.
evalScript(`function f8_1(){} var f8_2; { function f8_3(){} } { function f8_4(){} } var f8_5; function f8_6(){}`);
assertEquals([3, 4, 1, 6, 2, 5], Object.getOwnPropertyNames(this).filter(n => n.startsWith("f8_")).map(n => +n.replace("f8_", "")));

// Global function binding is initialized with "undefined".
evalScript(`
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
