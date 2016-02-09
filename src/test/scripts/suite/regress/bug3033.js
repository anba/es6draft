/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertFalse, assertTrue
} = Assert;

// 14.4.14 Evaluation, yield*: Handle Return abrupt completions
// https://bugs.ecmascript.org/show_bug.cgi?id=3033

{
  function* g() {
    let iter = {
      [Symbol.iterator]() {
        return this;
      },
      next() {
        return {value: 0, done: false};
      },
    };
    yield* iter;
    throw new Error("unreachable");
  }
  let gen = g();
  assertEquals({value: 0, done: false}, gen.next());
  assertEquals({value: -1, done: true}, gen.return(-1));
  assertEquals({value: void 0, done: true}, gen.next());
}

{
  let returnCalled = false, normalComplete = false;
  function* g() {
    let iter = {
      [Symbol.iterator]() {
        return this;
      },
      next() {
        return {value: 0, done: false};
      },
      return() {
        returnCalled = true;
        return {value: -2, done: true};
      }
    };
    let v = yield* iter;
    assertSame(-2, v);
    normalComplete = true;
    return -3;
  }
  let gen = g();
  assertEquals({value: 0, done: false}, gen.next());
  assertFalse(normalComplete);
  assertFalse(returnCalled);
  assertEquals({value: -2, done: true}, gen.return(-1));
  assertTrue(returnCalled);
  assertFalse(normalComplete);
  assertEquals({value: void 0, done: true}, gen.next());
}

{
  let returnCalled = false, normalComplete = false;
  function* g() {
    let iter = {
      [Symbol.iterator]() {
        return this;
      },
      next() {
        if (returnCalled) {
          return {value: -3, done: true};
        }
        return {value: 0, done: false};
      },
      return() {
        returnCalled = true;
        // 'done' value is not ignored
        return {value: -2, done: false};
      }
    };
    let v = yield* iter;
    assertSame(-3, v);
    normalComplete = true;
    return -4;
  }
  let gen = g();
  assertEquals({value: 0, done: false}, gen.next());
  assertFalse(normalComplete);
  assertFalse(returnCalled);
  assertEquals({value: -2, done: false}, gen.return(-1));
  assertEquals({value: -4, done: true}, gen.next());
  assertTrue(returnCalled);
  assertTrue(normalComplete);
  assertEquals({value: void 0, done: true}, gen.next());
}
