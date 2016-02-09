/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertThrows, fail
} = Assert;

// yield* broken for throw()
// https://bugs.ecmascript.org/show_bug.cgi?id=3526

function* g() {
  yield 1;
  try {
    yield 2;
    fail `unreachable`;
  } catch (e) {
    yield 3;
  }
  yield 4;
  return 5;
}

function* wrap(iter) {
  return yield* iter;
}

{
  let it = g();
  assertEquals({value: 1, done: false}, it.next());
  assertEquals({value: 2, done: false}, it.next());
  assertEquals({value: 3, done: false}, it.throw(new Error));
  assertEquals({value: 4, done: false}, it.next());
  assertEquals({value: 5, done: true}, it.next());
  assertEquals({value: void 0, done: true}, it.next());
}

{
  class Err extends Error {}

  let it = wrap(g());
  assertEquals({value: 1, done: false}, it.next());
  assertEquals({value: 2, done: false}, it.next());
  assertEquals({value: 3, done: false}, it.throw(new Error));
  assertEquals({value: 4, done: false}, it.next());
  assertEquals({value: 5, done: true}, it.next());
  assertEquals({value: void 0, done: true}, it.next());
}
