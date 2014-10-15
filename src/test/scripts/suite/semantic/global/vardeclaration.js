/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals,
  assertTrue,
} = Assert;


function testDuplicateVar(global, extra) {
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      log.push(`has(${pk})`);
      return pk in t;
    }
  }));
  var source = "var x, x;" + extra;
  try {
    (1, eval)(source);
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
    assertTrue(delete x);
  }
  assertEquals(["has(x)", "has(x)", "has(x)"], log);
}

// Test interpreted and compiled code.
testDuplicateVar(this, "");
testDuplicateVar(this, "() => {}");


function testVarListOrder(global, extra) {
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      log.push(`has(${pk})`);
      return pk in t;
    }
  }));
  var source = "var b, a;" + extra;
  try {
    (1, eval)(source);
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
    assertTrue(delete a);
    assertTrue(delete b);
  }
  assertEquals(["has(b)", "has(a)", "has(b)", "has(a)"], log);
}

// Test interpreted and compiled code.
testVarListOrder(this, "");
testVarListOrder(this, "() => {}");
