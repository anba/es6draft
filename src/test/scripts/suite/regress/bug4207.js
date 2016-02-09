/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertTrue, assertThrows, fail
} = Assert;

// 14.4.14 Evaluation: Step 6.b.iv.4 is unreachable
// https://bugs.ecmascript.org/show_bug.cgi?id=4207

function* g(iterable) {
  yield* iterable;
}

var throwCalled = false;
var returnCalled = false;

var iterator = {
  next() {
    return {value: 0, done: false};
  },
  get throw() {
    assertFalse(returnCalled);
    assertFalse(throwCalled);
    throwCalled = true;
    return null;
  },
  return(v) {
    assertSame(void 0, v);
    assertTrue(throwCalled);
    assertFalse(returnCalled);
    returnCalled = true;
    return {
      get value() { fail `.value accessed` },
      get done() { fail `.done accessed` },
    };
  },
  [Symbol.iterator]() {
    return this;
  },
};

var it = g(iterator);
it.next();

assertFalse(throwCalled);
assertFalse(returnCalled);
assertThrows(TypeError, () => it.throw(-1));
assertTrue(throwCalled);
assertTrue(returnCalled);
