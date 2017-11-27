/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertFalse, assertTrue
} = Assert;

// Annex E: Changed evaluation order in for-in statement
// https://bugs.ecmascript.org/show_bug.cgi?id=4352

var finallyExecuted;
function* g() {
  try {
    yield;
  } finally {
    assertFalse(finallyExecuted);
    finallyExecuted = true;
  }
}

finallyExecuted = false;
assertThrows(TypeError, () => {
  for (null[0] of g()) ;
});
assertTrue(finallyExecuted);


finallyExecuted = false;
class MyError extends Error { }
assertThrows(MyError, () => {
  var thrower = {
    set prop(v) {
      throw new MyError();
    }
  };
  for (thrower.prop of g()) ;
});
assertTrue(finallyExecuted);
