/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue
} = Assert;

// 14.5.11 NonConstructorMethodDefinitions: Missing IsStatic check
// https://bugs.ecmascript.org/show_bug.cgi?id=2898

{
  let constructorCalled = false;
  class A {
    constructor() {
      constructorCalled = true;
    }
  }
  assertFalse(constructorCalled);
  new A;
  assertTrue(constructorCalled);
}

{
  let constructorCalled = false;
  class B {
    static constructor() {
      constructorCalled = true;
    }
  }
  assertFalse(constructorCalled);
  new B;
  assertFalse(constructorCalled);
}

{
  let constructorCalled = false;
  class C {
    static constructor() {
      constructorCalled = true;
    }
  }
  assertFalse(constructorCalled);
  C.constructor();
  assertTrue(constructorCalled);
}
