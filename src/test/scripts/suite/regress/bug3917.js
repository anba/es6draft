/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse, assertThrows, fail
} = Assert;

// 12.14.5.3 IteratorDestructuringAssignmentEvaluation: iteratorRecord.[[done]] not set in elision when iterator completed
// https://bugs.ecmascript.org/show_bug.cgi?id=3917

// Normal completion
var nextCalled = false;
var iter = {
  [Symbol.iterator]() {
    return this;
  },
  next() {
    assertFalse(nextCalled);
    nextCalled = true;
    return {done: true};
  },
  return() {
    fail `return called`;
  }
};

assertFalse(nextCalled);
[,] = iter;
assertTrue(nextCalled);

// Throw completion
class ThrowStep extends Error { }

var nextCalled = false;
var throwIter = {
  [Symbol.iterator]() {
    return this;
  },
  next() {
    assertFalse(nextCalled);
    nextCalled = true;
    throw new ThrowStep;
  },
  return() {
    fail `return called`;
  }
};

assertFalse(nextCalled);
assertThrows(ThrowStep, () => [,] = throwIter);
assertTrue(nextCalled);
