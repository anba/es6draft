/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

/**
 * Compatibility layer for mocha bdd test API
 */

function reportFailure(reason) {
  return $async_enqueueJob(() => { throw reason });
}

function queueJob(job) {
  return $async_enqueueJob(job);
}

global.done = function done() {
  return reportFailure(new Error("invalid call to done"));
}

let asyncTestCount = 0;

function callDone(testCase) {
  if (--asyncTestCount <= 0) {
    $async_done();
  }
}

function registerAsyncTest(testCase) {
  ++asyncTestCount;
}

function isAsyncTest(test) {
  return test.length > 0;
}

class TestCase {
  constructor(suite, message, test, async = isAsyncTest(test)) {
    Object.assign(this, {suite, message, test, async});
    if (async) registerAsyncTest(this);
  }

  testSuccess() {
    return this.suite.testFinished(this);
  }

  testFailure(e) {
    if (!(e instanceof Error)) {
      e = new Error(`(${e})`);
    }
    return this.suite.testFinished(this, e);
  }

  execute() {
    return this.async ? this.executeAsync() : this.executeSync();
  }

  executeSync() {
    try {
      this.test();
    } catch (e) {
      return this.testFailure(e);
    }
    return this.testSuccess();
  }

  executeAsync() {
    try {
      let called = false;
      this.test(e => {
        if (!called) {
          called = true;
          if (e == null) {
            return this.testSuccess();
          } else {
            return this.testFailure(e);
          }
        }
      });
    } catch (e) {
      return this.testFailure(e);
    }
  }

  toString() {
    return `[${this.suite.toString()}]  ${this.message}`;
  }
}

class TestSuite {
  constructor(parent, message) {
    Object.assign(this, {parent, message});
    this.queue = [];
    this.inTest = false;
    this.before = () => {};
    this.after = () => {};
  }

  runTest(message, test) {
    let testCase = new TestCase(this, message, test);
    if (!this.inTest) {
      this.executeTest(testCase);
      this.drainQueue();
    } else {
      this.queue.push(testCase);
    }
  }

  executeTest(testCase) {
    this.inTest = true;
    try { this.before(); } catch (e) {}
    return testCase.execute();
  }

  testFinished(testCase, e) {
    try { this.after(); } catch (e) {}
    this.inTest = false;
    if (e) {
      reportFailure(e);
    }
    if (testCase.async) {
      callDone(testCase);
      queueJob(() => { this.drainQueue() });
    }
  }

  drainQueue() {
    while (!this.inTest && this.queue.length) {
      this.executeTest(this.queue.shift());
    }
  }

  toString() {
    return this.parent ? `${this.parent.toString()}: ${this.message}` : this.message;
  }
}

let suite = new TestSuite(null, "");

Object.assign(exports, {
  describe(message, tests) {
    suite = new TestSuite(suite, message);
    try {
      tests();
    } finally {
      suite = suite.parent;
    }
  },
  specify(message, test) {
    return suite.runTest(message, test);
  },
  beforeEach(t) {
    suite.before = t;
  },
  afterEach(t) {
    suite.after = t;
  },
});

// Alias for specify()
exports.it = exports.specify;
