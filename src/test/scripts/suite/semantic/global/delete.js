/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals,
  assertTrue,
} = Assert;


function testDeleteGlobalUnresolvedRef(global) {
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      log.push(`has(${pk})`);
      return false;
    }
  }));
  try {
    assertTrue(delete unresolvedRef);
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
  }
  assertEquals(["has(unresolvedRef)"], log);
}

testDeleteGlobalUnresolvedRef(this);


function testDeleteGlobalResolvedRef(global) {
  var log = [];
  Object.setPrototypeOf(global, new Proxy({}, {
    has(t, pk) {
      log.push(`has(${pk})`);
      return true;
    }
  }));
  try {
    assertTrue(delete resolvedRef);
  } finally {
    Object.setPrototypeOf(global, Object.prototype);
  }
  assertEquals(["has(resolvedRef)"], log);
}

testDeleteGlobalResolvedRef(this);
