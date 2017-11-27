/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertThrows, fail
} = Assert;

// 14.4.14 Evaluation yield*: Semicolon, unused steps, abrupt completion
// https://bugs.ecmascript.org/show_bug.cgi?id=3179

class Err extends Error { }

var iter = {
  [Symbol.iterator]() {
    return this;
  },
  next() {
    return {value: 0, done: false};
  },
  return() {
    return {
      get done() {
        return true;
      },
      get value() {
        throw new Err;
      }
    };
  }
};

function* g() {
  yield* iter;
  fail `unreachable`;
}

var gen = g();
assertEquals({value: 0, done: false}, gen.next());
assertThrows(Err, () => gen.return(1));
assertEquals({value: 2, done: true}, gen.return(2));
