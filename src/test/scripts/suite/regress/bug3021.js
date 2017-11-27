/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse, assertThrows, fail
} = Assert;

// 8.1.1.4.2 CreateMutableBinding, 8.1.1.4.3 CreateImmutableBinding: Change ReturnIfAbrupt to if-condition + return
// https://bugs.ecmascript.org/show_bug.cgi?id=3021

// This test requires the non-standard evalScript function.
// Note: No longer reproducible without Reflect.Realm.

if (false) {
  let called = false;
  Object.setPrototypeOf(this, new Proxy({}, {
    has(t, pk) {
      if (pk === "varDecl") {
        assertFalse(called);
        called = true;
        try {
          evalScript("let letDecl");
        } catch (e) {
          fail `unexpected error`;
        }
      }
      return Reflect.has(t, pk);
    }
  }));
  assertThrows(TypeError, () => evalScript("var varDecl; let letDecl"));
  assertTrue(called);
  Object.setPrototypeOf(this, null);
}

if (false) {
  let called = false;
  Object.setPrototypeOf(this, new Proxy({}, {
    has(t, pk) {
      if (pk === "varDecl_2") {
        assertFalse(called);
        called = true;
        try {
          evalScript("const constDecl = 0");
        } catch (e) {
          fail `unexpected error`;
        }
      }
      return Reflect.has(t, pk);
    }
  }));
  assertThrows(TypeError, () => evalScript("var varDecl_2; const constDecl = 0"));
  assertTrue(called);
  Object.setPrototypeOf(this, null);
}
