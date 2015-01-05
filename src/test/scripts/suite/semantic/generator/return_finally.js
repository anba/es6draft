/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertFalse, assertTrue
} = Assert;

// Generator.prototype.return and try-finally

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
  let finallyExecuted = false;
  function* g() {
    try {
      yield 1;
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
}

// yield in finally block, return value still available
{
  function* g() {
    try {
      yield 1;
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
}

// yield in finally block, new return value
{
  function* g() {
    try {
      yield 1;
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
}

// return in finally block
{
  function* g() {
    try {
      yield 1;
    } finally {
      if (Math.random() >= 0) return 2;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
}

// break in finally block
{
  let finallyExited = false;
  function* g() {
    L: try {
      yield 1;
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
}

// break in finally block
{
  function* g() {
    L: try {
      yield 1;
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
}

// break in finally block
{
  function* g() {
    L: try {
      yield 1;
    } finally {
      break L;
    }
    return 2;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
}

// continue in finally block
{
  let finallyExited = false;
  function* g() {
    do try {
      yield 1;
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
}

// continue in finally block
{
  function* g() {
    do try {
      yield 1;
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
}

// continue in finally block
{
  function* g() {
    do try {
      yield 1;
    } finally {
      continue;
    } while (0);
    return 2;
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(2, gen.return(-1));
  assertIterDone(void 0, gen.next());
}

// nested finally blocks are executed
{
  let outerFinallyExecuted = false, innerFinallyExecuted = false;
  function* g() {
    try {
      try {
        yield 1;
      } finally {
        innerFinallyExecuted = true;
      }
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
}

// yield in nested finally blocks, return value still available (1)
{
  function* g() {
    try {
      try {
        yield 1;
      } finally {
        yield 2;
      }
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
}

// yield in nested finally blocks, return value still available (2)
{
  function* g() {
    try {
      try {
        yield 1;
      } finally {
        yield 2;
      }
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
}

// yield in nested finally blocks, new return value
{
  function* g() {
    try {
      try {
        yield 1;
      } finally {
        yield 2;
      }
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
}

// return in nested finally blocks
{
  function* g() {
    try {
      try {
        yield 1;
      } finally {
        if (Math.random() >= 0) return 2;
      }
    } finally {
      if (Math.random() >= 0) return 3;
    }
    throw new Error("unreachable");
  }

  let gen = g();
  assertIterNext(1, gen.next());
  assertIterDone(3, gen.return(-1));
  assertIterDone(void 0, gen.next());
}
