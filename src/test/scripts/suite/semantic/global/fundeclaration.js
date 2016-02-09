/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals,
  assertTrue,
  assertSame,
  assertUndefined,
  assertThrows,
  assertDataProperty,
} = Assert;

// Note: These tests are no longer reproducible, require Reflect.Realm support.

function testDuplicateFunction(global) {
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      log.push(`has(${pk})`);
      return pk in t;
    }
  }));
  var source = "function F(){ return 1 } function F(){ return 2 }";
  try {
    (1, eval)(source);
    assertSame(2, F());
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
    assertTrue(delete F);
  }
  assertEquals(["has(F)"], log);
}

// testDuplicateFunction(this);


function testFunReverseListOrder(global) {
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      log.push(`has(${pk})`);
      return pk in t;
    }
  }));
  var source = "function A(){} function B(){}";
  try {
    (1, eval)(source);
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
    assertTrue(delete A);
    assertTrue(delete B);
  }
  assertEquals(["has(B)", "has(A)"], log);
}

// testFunReverseListOrder(this);


function testFunInitOrder(global) {
  var propDesc = {value: 0, writable: false, enumerable: false, configurable: false};
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      if (pk === "X") {
        // Define "A" as non-configurable data property before "function A(){}" is installed.
        assertUndefined(Object.getOwnPropertyDescriptor(global, "A"));
        Object.defineProperty(global, "A", propDesc);
      }
      log.push(`has(${pk})`);
      return pk in t;
    }
  }));
  var source = "function A(){} function B(){} var X = 1;";
  try {
    assertThrows(TypeError, () => (1, eval)(source));
    assertDataProperty(global, "A", propDesc);
    assertUndefined(Object.getOwnPropertyDescriptor(global, "B"));
    assertUndefined(Object.getOwnPropertyDescriptor(global, "X"));
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
  }
  assertEquals(["has(B)", "has(A)", "has(X)"], log);
}

// testFunInitOrder(this);
