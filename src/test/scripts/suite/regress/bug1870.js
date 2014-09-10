/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, assertFalse, assertTrue
} = Assert;

// for-of iteration should not ToObject() returned iterator
// https://bugs.ecmascript.org/show_bug.cgi?id=1870

{
  let called = false;
  let iter = {
    [Symbol.iterator]() {
      assertFalse(called);
      called = true;
      return "";
    }
  };

  assertThrows(TypeError, () => { for (let v of iter) ; });
  assertTrue(called);
}

{
  let called = false;
  let iter = {
    [Symbol.iterator]() {
      assertFalse(called);
      called = true;
      return "";
    }
  };

  assertThrows(TypeError, () => { [for (v of iter) v] });
  assertTrue(called);
}
