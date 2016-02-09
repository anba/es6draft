/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertEquals, fail
} = Assert;

// 7.4.6 IteratorClose, 14.4.15 Evaluation yield*: HasProperty + Invoke vs. GetMethod
// https://bugs.ecmascript.org/show_bug.cgi?id=3695

class Iter {
  next() {
    return {value: 0, done: false};
  }
  [Symbol.iterator]() {
    return this;
  }
}
class NullReturn extends Iter {
  get return() {
    return null;
  }
  throw() {
    fail `called Iter::throw`;
  }
}
class NullThrow extends Iter {
  get throw() {
    return null;
  }
  return() {
    throw new NullThrowError();
  }
}
class NullThrowError extends Error { }

function* g(iter) {
  yield* iter;
}

var it = g(new NullReturn);
assertEquals({value: 0, done: false}, it.next());
assertEquals({value: -1, done: true}, it.return(-1));

var it = g(new NullThrow);
assertEquals({value: 0, done: false}, it.next());
assertThrows(NullThrowError, () => it.throw(new Error));
