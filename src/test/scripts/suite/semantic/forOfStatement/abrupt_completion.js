/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertFalse, assertEquals
} = Assert;

{
  function testIter(fn, result = void 0) {
    let returnCalled = false, nextCallCount = 0;
    let iter = {
      [Symbol.iterator]() {
        return this;
      },
      next() {
        nextCallCount += 1;
        return {value: 0, done: false};
      },
      return(...args) {
        assertSame(0, args.length);
        returnCalled = true;
      }
    };
    let rval = fn(iter);
    assertTrue(returnCalled);
    assertSame(1, nextCallCount);
    assertSame(result, rval);
  }
  function testGen(g, ival) {
    testIter(iter => {
      let gen = g(iter);
      assertEquals({value: ival, done: false}, gen.next());
      assertEquals({value: -1, done: true}, gen.return(-1));
    });
  }

  // return, no expression
  testIter(iter => { for (let v of iter) return; });

  // return, with expression
  testIter(iter => { for (let v of iter) return 123; }, 123);

  // break
  testIter(iter => { for (let v of iter) break; });

  // outer break
  testIter(iter => { L: { for (let v of iter) break L; } });

  // break + return
  testIter(iter => { for (let v of iter) { if (Math.random() >= 0) break; return; } });

  // return + break
  testIter(iter => { for (let v of iter) { if (Math.random() >= 0) return; break; } });

  // outer break + return
  testIter(iter => { L: { for (let v of iter) { if (Math.random() >= 0) break L; return; } } });

  // return + outer break
  testIter(iter => { L: { for (let v of iter) { if (Math.random() >= 0) return; break L; } } });

  // yield, no expression
  testGen(function* g(iter) { for (let v of iter) yield; throw new Error("unreachable"); }, void 0);

  // yield, with expression
  testGen(function* g(iter) { for (let v of iter) yield v; throw new Error("unreachable"); }, 0);
}

{
  function testIter(fn, result = void 0, handler = () => {}) {
    let throwCalled = false, nextCallCount = 0;
    let iter = {
      [Symbol.iterator]() {
        return this;
      },
      next() {
        nextCallCount += 1;
        return {value: 0, done: false};
      },
      throw(...args) {
        assertSame(1, args.length);
        throwCalled = true;
        handler(args[0]);
      }
    };
    let rval;
    try { fn(iter); } catch (e) { rval = e; }
    assertTrue(throwCalled);
    assertSame(1, nextCallCount);
    assertSame(result, rval);
  }
  function testGen(g, ival) {
    testIter(iter => {
      let gen = g(iter);
      assertEquals({value: ival, done: false}, gen.next());
      gen.throw(-1);
    }, -1);
    testIter(iter => {
      let gen = g(iter);
      assertEquals({value: ival, done: false}, gen.next());
      gen.throw(-1);
    }, -2, e => { throw e * 2 });
  }

  // throw
  testIter(iter => { for (let v of iter) throw 123; }, 123);
  testIter(iter => { for (let v of iter) throw 123; }, 123 * 2, e => { throw e * 2 });

  // yield, no expression
  testGen(function* g(iter) { for (let v of iter) yield; throw new Error("unreachable"); }, void 0);

  // yield, with expression
  testGen(function* g(iter) { for (let v of iter) yield v; throw new Error("unreachable"); }, 0);
}

// Generators (return)
function drainReturn(g) {
  for (let v of g()) return v;
}

{
  function* g() {
    yield 0;
    throw new Error("unreachable");
  }
  let rval = drainReturn(g);
  assertSame(0, rval);
}

{
  let finallyExecuted = false;
  function* g() {
    try {
      yield 0;
    } finally {
      finallyExecuted = true;
      return 1;
    }
  }
  let rval = drainReturn(g);
  assertSame(0, rval);
  assertTrue(finallyExecuted);
}

{
  let finallyExecuted = false;
  function* g() {
    try {
      yield 0;
    } finally {
      finallyExecuted = true;
      throw 1;
    }
  }
  let caught;
  try {
    drainReturn(g);
  } catch (e) {
    caught = e;
  }
  assertSame(1, caught);
  assertTrue(finallyExecuted);
}

{
  let finallyExecuted = false;
  function* g() {
    try {
      yield 0;
    } finally {
      finallyExecuted = true;
      yield 1;
    }
  }
  let rval = drainReturn(g);
  assertSame(0, rval);
  assertTrue(finallyExecuted);
}

// Generators (break)
function drainBreak(g) {
  let v;
  for (v of g()) break;
  return v;
}

{
  function* g() {
    yield 0;
    throw new Error("unreachable");
  }
  let rval = drainBreak(g);
  assertSame(0, rval);
}

{
  let finallyExecuted = false;
  function* g() {
    try {
      yield 0;
    } finally {
      finallyExecuted = true;
      return 1;
    }
  }
  let rval = drainBreak(g);
  assertSame(0, rval);
  assertTrue(finallyExecuted);
}

{
  let finallyExecuted = false;
  function* g() {
    try {
      yield 0;
    } finally {
      finallyExecuted = true;
      throw 1;
    }
  }
  let caught;
  try {
    drainBreak(g);
  } catch (e) {
    caught = e;
  }
  assertSame(1, caught);
  assertTrue(finallyExecuted);
}

{
  let finallyExecuted = false;
  function* g() {
    try {
      yield 0;
    } finally {
      finallyExecuted = true;
      yield 1;
    }
  }
  let rval = drainBreak(g);
  assertSame(0, rval);
  assertTrue(finallyExecuted);
}
