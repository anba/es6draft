/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 14.4.15 Evaluation: Missing ReturnIfAbrupt
// https://bugs.ecmascript.org/show_bug.cgi?id=3769

class ReturnError extends Error { }
class ThrowError extends Error { }

var iterable = {
  [Symbol.iterator]() {
    return this;
  },
  next() {
    return {value: 0, done: false};
  },
  return() {
    return {
      get value() {
        throw new ReturnError();
      },
      done: true
    };
  },
  throw() {
    return {
      get value() {
        throw new ThrowError();
      },
      done: true
    };
  },
};

function* g() {
  yield* iterable;
}

var it = g();
it.next();
assertThrows(ReturnError, () => it.return());

var it = g();
it.next();
assertThrows(ThrowError, () => it.throw());
