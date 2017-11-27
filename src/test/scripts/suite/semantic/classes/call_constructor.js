/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  fail, assertSame, assertSyntaxError, assertThrows, assertUndefined
} = Assert;

// 'call' and 'constructor' must be names without escape characters.
assertSyntaxError(`class C { "call" constructor() {} }`);
assertSyntaxError(`class C { call "constructor"() {} }`);
assertSyntaxError(String.raw `class C { \u0063all constructor() {} }`);
assertSyntaxError(String.raw `class C { call \u0063onstructor() {} }`);

// early errors: duplicate call constructor not allowed.
assertSyntaxError(`class C { call constructor() {} call constructor() {} }`);

// early errors: strict formal parameters and redefinitions.
assertSyntaxError(`class C { call constructor(x, x) {} }`);
assertSyntaxError(`class C { call constructor(x) {let x} }`);
assertSyntaxError(`class C { call constructor([x]) {let x} }`);
assertSyntaxError(`class C { call constructor({x}) {let x} }`);
assertSyntaxError(`class C { call constructor(x) {const x = 0} }`);
assertSyntaxError(`class C { call constructor([x]) {const x = 0} }`);
assertSyntaxError(`class C { call constructor({x}) {const x = 0} }`);

// early errors: super() not allowed in call constructor definition.
assertSyntaxError(`class C { call constructor() { super() } }`);
assertSyntaxError(`class C extends Object { call constructor() { super() } }`);
assertSyntaxError(`class C { call constructor(x = super()) { } }`);
assertSyntaxError(`class C extends Object { call constructor(x = super()) { } }`);

// Decorator expression not allowed in call constructor definition.
assertSyntaxError(`class C { @D call constructor() { } }`);

// Simple call constructor tests.
{
  class SimpleClassNoConstructor {
    call constructor() {
      return "ok";
    }
  }
  assertSame("ok", SimpleClassNoConstructor());

  class SimpleClassWithConstructor {
    constructor() {
      fail `constructor called`;
    }
    call constructor() {
      return "ok";
    }
  }
  assertSame("ok", SimpleClassWithConstructor());

  class DerivedClassNoConstructor extends Object {
    call constructor() {
      return "ok";
    }
  }
  assertSame("ok", DerivedClassNoConstructor());

  class DerivedClassWithConstructor extends Object {
    constructor() {
      fail `constructor called`;
    }
    call constructor() {
      return "ok";
    }
  }
  assertSame("ok", DerivedClassWithConstructor());
}

// Simple call constructor tests with argument.
{
  class SimpleClassNoConstructor {
    call constructor(x) {
      return x;
    }
  }
  assertSame("ok", SimpleClassNoConstructor("ok"));

  class SimpleClassWithConstructor {
    constructor() {
      fail `constructor called`;
    }
    call constructor(x) {
      return x;
    }
  }
  assertSame("ok", SimpleClassWithConstructor("ok"));

  class DerivedClassNoConstructor extends Object {
    call constructor(x) {
      return x;
    }
  }
  assertSame("ok", DerivedClassNoConstructor("ok"));

  class DerivedClassWithConstructor extends Object {
    constructor() {
      fail `constructor called`;
    }
    call constructor(x) {
      return x;
    }
  }
  assertSame("ok", DerivedClassWithConstructor("ok"));
}

// call constructor is not inherited.
{
  class Base { call constructor() { fail `invoked` } }
  class Derived extends Base { }
  assertThrows(TypeError, () => Derived());
}

// call constructors are strict mode functions, test this-binding.
{
  class Base { call constructor() { return this } }
  class Derived extends Object { call constructor() { return this } }

  assertSame(null, Base.call(null));
  assertSame(null, Derived.call(null));
}

// call constructors return undefined if no 'return' statement is present.
{
  class Base { call constructor() { } }
  class Derived extends Object { call constructor() { } }

  assertUndefined(Base());
  assertUndefined(Derived());
}

// Tail call works
{
  function returnsCaller() { return returnsCaller.caller }
  class TailCall { call constructor() { return returnsCaller() } }
  function testTailCall() {
    assertSame(testTailCall, TailCall());
  }
  testTailCall();
}
