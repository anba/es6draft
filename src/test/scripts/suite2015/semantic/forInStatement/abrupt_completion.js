/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertFalse, assertEquals
} = Assert;

function toEnumerator(iter) {
  return new Proxy({}, {enumerate: () => iter});
}

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
        return {};
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
  testIter(iter => { for (let v in toEnumerator(iter)) return; });

  // return, with expression
  testIter(iter => { for (let v in toEnumerator(iter)) return 123; }, 123);

  // break
  testIter(iter => { for (let v in toEnumerator(iter)) break; });

  // outer break
  testIter(iter => { L: { for (let v in toEnumerator(iter)) break L; } });

  // outer continue
  testIter(iter => { L: do { for (let v in toEnumerator(iter)) continue L; } while(0); });

  // break + return
  testIter(iter => { for (let v in toEnumerator(iter)) { if (Math.random() >= 0) break; return; } });

  // return + break
  testIter(iter => { for (let v in toEnumerator(iter)) { if (Math.random() >= 0) return; break; } });

  // outer break + return
  testIter(iter => { L: { for (let v in toEnumerator(iter)) { if (Math.random() >= 0) break L; return; } } });

  // return + outer break
  testIter(iter => { L: { for (let v in toEnumerator(iter)) { if (Math.random() >= 0) return; break L; } } });

  // outer continue + return
  testIter(iter => { L: do { for (let v in toEnumerator(iter)) { if (Math.random() >= 0) continue L; return; } } while(0); });

  // return + outer continue
  testIter(iter => { L: do { for (let v in toEnumerator(iter)) { if (Math.random() >= 0) return; continue L; } } while(0); });

  // yield, no expression
  testGen(function* g(iter) { for (let v in toEnumerator(iter)) yield; throw new Error("unreachable"); }, void 0);

  // yield, with expression
  testGen(function* g(iter) { for (let v in toEnumerator(iter)) yield v; throw new Error("unreachable"); }, 0);
}

{
  function testIter(fn, result = void 0, handler = () => {}) {
    let returnCalled = false, nextCallCount = 0, argsLength = -1;
    let iter = {
      [Symbol.iterator]() {
        return this;
      },
      next() {
        nextCallCount += 1;
        return {value: 0, done: false};
      },
      return(...args) {
        // Exception within close action are ignored!
        // assertSame(1, args.length);
        argsLength = args.length;
        returnCalled = true;
        handler(args[0]);
        return {};
      }
    };
    let rval;
    try { fn(iter); } catch (e) { rval = e; }
    assertTrue(returnCalled);
    assertSame(0, argsLength);
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
    }, -1, e => { throw -2 });
  }

  // throw
  testIter(iter => { for (let v in toEnumerator(iter)) throw 123; }, 123);
  testIter(iter => { for (let v in toEnumerator(iter)) throw 123; }, 123, e => { throw 456 });

  // yield, no expression
  testGen(function* g(iter) { for (let v in toEnumerator(iter)) yield; throw new Error("unreachable"); }, void 0);

  // yield, with expression
  testGen(function* g(iter) { for (let v in toEnumerator(iter)) yield v; throw new Error("unreachable"); }, 0);
}

// Generators (return)
function drainReturn(g) {
  for (let v in toEnumerator(g())) return v;
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
  for (v in toEnumerator(g())) break;
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

// Generators (continue)
function drainContinue(g) {
  let v;
  L: do for (v in toEnumerator(g())) continue L; while(0);
  return v;
}

{
  function* g() {
    yield 0;
    throw new Error("unreachable");
  }
  let rval = drainContinue(g);
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
  let rval = drainContinue(g);
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
    drainContinue(g);
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
  let rval = drainContinue(g);
  assertSame(0, rval);
  assertTrue(finallyExecuted);
}
