/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue, assertFalse, assertThrows, fail
} = Assert;

// 8.1.1.4.2 CreateMutableBinding, 8.1.1.4.3 CreateImmutableBinding: Change ReturnIfAbrupt to if-condition + return
// https://bugs.ecmascript.org/show_bug.cgi?id=3021

{
  let called = false;
  Object.setPrototypeOf(this, new Proxy({}, {
    has(t, pk) {
      if (pk === "varDecl") {
        assertFalse(called);
        called = true;
        try {
          (1, eval)("let letDecl");
        } catch (e) {
          fail `unexpected error`;
        }
      }
      return Reflect.has(t, pk);
    }
  }));
  assertThrows(TypeError, () => (1, eval)("var varDecl; let letDecl"));
  assertTrue(called);
  Object.setPrototypeOf(this, null);
}

{
  let called = false;
  Object.setPrototypeOf(this, new Proxy({}, {
    has(t, pk) {
      if (pk === "varDecl_2") {
        assertFalse(called);
        called = true;
        try {
          (1, eval)("const constDecl = 0");
        } catch (e) {
          fail `unexpected error`;
        }
      }
      return Reflect.has(t, pk);
    }
  }));
  assertThrows(TypeError, () => (1, eval)("var varDecl_2; const constDecl = 0"));
  assertTrue(called);
  Object.setPrototypeOf(this, null);
}
