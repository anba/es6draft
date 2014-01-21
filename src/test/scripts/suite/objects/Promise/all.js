/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  fail,
  assertSame, assertEquals,
  assertFalse, assertTrue,
} = Assert;

loadRelativeToScript("../../lib/promises.js");

const {
  reportFailure
} = Promises;

/* Promise.all ( iterable ) */

assertBuiltinFunction(Promise.all, "all", 1);

// Empty iterables
{
  for (let iterable of [[], function*(){}(), {[Symbol.iterator]: () => ({next: () => ({done: true})})}]) {
    let count = 0;
    Promise
      .all(iterable)
      .then(values => {
        assertSame(0, count++);
        assertEquals([], values);
      })
      .catch(reportFailure);
    Promise.resolve().then(() => assertSame(1, count)).catch(reportFailure);
  }
}

// Promise.all and rejected promises
{
  // No .then() for rejected promises
  Promise
    .all([Promise.reject(0)])
    .then(v => fail `fulfilled with ${v}`, v => { assertSame(0, v) })
    .catch(reportFailure);
  Promise
    .all([Promise.reject(0), Promise.resolve(1)])
    .then(v => fail `fulfilled with ${v}`, v => { assertSame(0, v) })
    .catch(reportFailure);
  Promise
    .all([Promise.resolve(1), Promise.reject(0)])
    .then(v => fail `fulfilled with ${v}`, v => { assertSame(0, v) })
    .catch(reportFailure);
}

function tamper(p) {
  return Object.assign(p, {
    then(onFulfilled, onRejected) {
      onFulfilled();
      return Promise.prototype.then.call(this, onFulfilled, onRejected);
    }
  });
}

// Manual dispatch of Promise.all Countdown Functions (1)
{
  function* g() {
    yield tamper(Promise.resolve(0));
  }

  // Prevent countdownHolder.[[Countdown]] from ever reaching zero
  Promise
    .all(g())
    .then(() => { throw new Error("resolve called") })
    .catch(reportFailure);
}

// Manual dispatch of Promise.all Countdown Functions (2)
{
  function* g() {
    yield Promise.resolve(0);
    yield tamper(Promise.resolve(1));
    yield Promise.resolve(2)
      .then(() => {
        assertFalse(fulfillCalled);
      })
      .then(() => {
        assertTrue(fulfillCalled);
      })
      .catch(reportFailure);
  }

  // Promise from Promise.all resolved before arguments
  let fulfillCalled = false;
  Promise
    .all(g())
    .then(() => {
      assertFalse(fulfillCalled);
      fulfillCalled = true;
    })
    .catch(reportFailure);
}

// Manual dispatch of Promise.all Countdown Functions (3)
{
  function* g() {
    yield Promise.resolve(0);
    yield tamper(Promise.resolve(1));
    yield Promise.reject(2);
  }

  // Promise from Promise.all resolved despite rejected promise in arguments
  Promise
    .all(g())
    .then(v => {
      assertEquals([0, 1], v);
    }, reportFailure)
    .catch(reportFailure);
}

// Manual dispatch of Promise.all Countdown Functions (4)
{
  let hijack = true;
  class P extends Promise {
    constructor(resolver) {
      if (hijack) {
        hijack = false;
        super((resolve, reject) => {
          return resolver(values => {
            actualArguments.push(values.slice());
            return resolve(values);
          }, reject);
        });
      } else {
        super(resolver);
      }
    }

    static cast(p) {
      return p;
    }
  }
  function* g() {
    yield Promise.resolve(0);
    yield tamper(Promise.resolve(1));
    yield Promise.resolve(2);
  }
  let actualArguments = [];
  let expectedArguments = [];
  expectedArguments.push([, void 0]);
  expectedArguments.push([0, 1]);

  // Promise.all calls resolver twice
  P.all(g()).catch(reportFailure);
  Promise
    .resolve()
    .then(() => assertEquals(expectedArguments, actualArguments))
    .catch(reportFailure);
}
