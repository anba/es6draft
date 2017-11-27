/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

// semantics of yield* in throw case
// https://github.com/tc39/ecma262/pull/293

var err;

function* g() {
  try {
    yield 2;
  } catch (e) {
    err = e;
    return;
  }
}

function* wrap(generator) {
  yield 1;
  yield* generator();
  yield 3;
  yield 4;
}

var it = wrap(g);

assertEquals({value: 1, done: false}, it.next());
assertEquals({value: 2, done: false}, it.next());
assertSame(void 0, err);
assertEquals({value: 3, done: false}, it.throw("error"));
assertSame("error", err);
assertEquals({value: 4, done: false}, it.next());
assertEquals({value: void 0, done: true}, it.next());
