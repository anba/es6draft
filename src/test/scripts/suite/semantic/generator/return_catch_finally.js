/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertFalse, assertTrue
} = Assert;

// Generator.prototype.return and try-catch-finally

function assertIterNext(value, actual) {
  "use strict";
  return assertEquals({value, done: false}, actual);
}
function assertIterDone(value, actual) {
  "use strict";
  return assertEquals({value, done: true}, actual);
}

// finally block is executed
{
  let finallyExecuted = false, catchExecuted = false;
  function* g() {
    try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      finallyExecuted = true;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertFalse(finallyExecuted);
  assertIterDone(-1, gen.return(-1));
  assertTrue(finallyExecuted);
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// yield in finally block, return value still available
{
  let catchExecuted = false;
  function* g() {
    try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      yield 2;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterDone(-1, gen.next());
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// yield in finally block, new return value
{
  let catchExecuted = false;
  function* g() {
    try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      yield 2;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterDone(-2, gen.return(-2));
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// return in finally block
{
  let catchExecuted = false;
  function* g() {
    try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      if (Math.random() >= 0) return 2;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// break in finally block
{
  let catchExecuted = false, finallyExited = false;
  function* g() {
    L: try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      break L;
    }
    finallyExited = true;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertFalse(finallyExited);
  assertIterDone(void 0, gen.return(-1));
  assertTrue(finallyExited);
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// break in finally block
{
  let catchExecuted = false;
  function* g() {
    L: try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      break L;
    }
    yield 2;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// break in finally block
{
  let catchExecuted = false;
  function* g() {
    L: try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      break L;
    }
    return 2;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// continue in finally block
{
  let catchExecuted = false, finallyExited = false;
  function* g() {
    do try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      continue;
    } while (0);
    finallyExited = true;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertFalse(finallyExited);
  assertIterDone(void 0, gen.return(-1));
  assertTrue(finallyExited);
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// continue in finally block
{
  let catchExecuted = false;
  function* g() {
    do try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      continue;
    } while (0);
    yield 2;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// continue in finally block
{
  let catchExecuted = false;
  function* g() {
    do try {
      yield 1;
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      continue;
    } while (0);
    return 2;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// nested finally blocks are executed
{
  let catchExecuted = false, outerFinallyExecuted = false, innerFinallyExecuted = false;
  function* g() {
    try {
      try {
        yield 1;
      } catch (e) {
        if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
      } finally {
        innerFinallyExecuted = true;
      }
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      outerFinallyExecuted = true;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertFalse(outerFinallyExecuted);
  assertFalse(innerFinallyExecuted);
  assertIterDone(-1, gen.return(-1));
  assertTrue(outerFinallyExecuted);
  assertTrue(innerFinallyExecuted);
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// yield in nested finally blocks, return value still available (1)
{
  let catchExecuted = false;
  function* g() {
    try {
      try {
        yield 1;
      } catch (e) {
        if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
      } finally {
        yield 2;
      }
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      yield 3;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterNext(3, gen.next());
  assertIterDone(-1, gen.next());
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// yield in nested finally blocks, return value still available (2)
{
  let catchExecuted = false;
  function* g() {
    try {
      try {
        yield 1;
      } catch (e) {
        if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
      } finally {
        yield 2;
      }
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      yield 3;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterNext(3, gen.return(-2));
  assertIterDone(-2, gen.next());
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// yield in nested finally blocks, new return value
{
  let catchExecuted = false;
  function* g() {
    try {
      try {
        yield 1;
      } catch (e) {
        if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
      } finally {
        yield 2;
      }
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      yield 3;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterNext(2, gen.return(-1));
  assertIterNext(3, gen.return(-2));
  assertIterDone(-3, gen.return(-3));
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}

// return in nested finally blocks
{
  let catchExecuted = false;
  function* g() {
    try {
      try {
        yield 1;
      } catch (e) {
        if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
      } finally {
        if (Math.random() >= 0) return 2;
      }
    } catch (e) {
      if (Math.random() >= 0) { catchExecuted = true; throw new Error("unreachable"); }
    } finally {
      if (Math.random() >= 0) return 3;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(3, gen.return(-1));
  assertIterDone(void 0, gen.next());
  assertFalse(catchExecuted);
}
