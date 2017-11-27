/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertTrue, assertThrows, fail
} = Assert;

// Private fields don't work with `extends null` class heritage.

// explicit constructor super() cannot be called when `extends null` is used.
{
  let pre = false, post = false;
  class C extends null {
    #private = fail();
    constructor() {
      pre = true;
      super(); // <- throws TypeError
      post = true;
    }
  }
  assertFalse(pre);
  assertFalse(post);
  assertThrows(TypeError, () => new C());
  assertTrue(pre);
  assertFalse(post);
}

// needs explicit constructor to initialize this-value.
{
  class C extends null {
    #private = fail();
  }
  assertThrows(TypeError, () => new C());
}

// explicit constructor needs to return object to initialize this-value.
{
  class C extends null {
    #private = fail();

    constructor() {}

    m() {
      return this.#private;
    }
  }
  assertThrows(ReferenceError, () => new C());
}

// private field not assigned with explicit return value.
{
  let methodCalled = false;
  class C extends null {
   #private = "ok";

   constructor() {
     return Object.create(C.prototype);
   }

   m() {
     assertFalse(methodCalled);
     methodCalled = true;
     return this.#private;
   }
  }
  let obj = new C();
  assertThrows(TypeError, () => obj.m());
  assertTrue(methodCalled);
}
