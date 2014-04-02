"use strict";

function iterableFromArray(array) {
  return array[Symbol.iterator]();
}

function delayPromise(value, ms) {
  return new Promise(function (resolve) {
    setTimeout(function () {
      resolve(value);
    }, ms);
  });
}

function OrdinaryConstruct(constructor, args) {
  return constructor.call(constructor[Symbol.create](), ...args);
}

var atAtIterator = Symbol.iterator;

/**
 * Basic implementation of the mocha bdd test API
 */
var {describe, specify, it, beforeEach} = (function BDD() {
  class TestSuite {
    constructor() {
      this.before = () => {};
    }

    runTest(message, test) {
      if (test.length === 1) {
        ++globalDoneCount;
        function callDone() {
          if (--globalDoneCount === 0) {
            done();
          }
        }
        this.before();
        return test(callDone);
      }
      this.before();
      return test();
    }
  }
  var globalDoneCount = 0;
  var suite = new TestSuite();

  function describe(message, tests) {
    suite = new TestSuite();
    return tests();
  }

  function specify(message, test) {
    return suite.runTest(message, test);
  }

  function beforeEach(t) {
    suite.before = t;
  }

  return {describe, specify, it: specify, beforeEach};
})();

/**
 * Minimum implementation of the node.js assert API
 */
var assert = (function Assert() {
  class AssertionError extends Error {
    get name() {
      return "AssertionError";
    }
  }
  function equal(actual, expected, message) {
    if (actual != expected) {
      throw new AssertionError(message);
    }
  }
  function strictEqual(actual, expected, message) {
    if (actual !== expected) {
      throw new AssertionError(message);
    }
  }
  function notStrictEqual(actual, expected, message) {
    if (actual === expected) {
      throw new AssertionError(message);
    }
  }
  function ok(value, message = "") {
    return equal(true, !!value, message);
  }
  function deepEqual(actual, expected, message) {
    strictEqual(typeof actual, typeof expected, message);
    if (!(typeof actual === 'object' && actual !== null || typeof actual === 'function')) {
      return strictEqual(actual, expected, message);
    }
    for (let name of Object.getOwnPropertyNames(actual)) {
      deepEqual(actual[name], expected[name], message);
    }
    for (let name of Object.getOwnPropertyNames(expected)) {
      deepEqual(actual[name], expected[name], message);
    }
  }
  function doesNotThrow(f) {
    try {
      return f();
    } catch (e) {
      return ok(false);
    }
  }
  return Object.assign(function assert(value, message) {
    return equal(true, !!value, message);
  }, {
    equal, strictEqual, notStrictEqual, ok, deepEqual, doesNotThrow
  });
})();
