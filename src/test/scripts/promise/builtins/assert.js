/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

/**
 * Compatibility layer for node.js assert API
 */

class AssertionError extends Error {
  get name() {
    return "AssertionError";
  }
}

function equal(actual, expected, message = "") {
  if (actual != expected) {
    throw new AssertionError(`[Equal] ${message}: actual=${actual}, expected=${expected}`);
  }
}

function strictEqual(actual, expected, message = "") {
  if (actual !== expected) {
    throw new AssertionError(`[StrictEqual] ${message}: actual=${actual}, expected=${expected}`);
  }
}

function notStrictEqual(actual, expected, message = "") {
  if (actual === expected) {
    throw new AssertionError(`[NotStrictEqual] ${message}: actual=${actual}, expected=${expected}`);
  }
}

function assert(value, message = "") {
  return equal(true, !!value, message);
}

function ok(value, message = "") {
  return equal(true, !!value, message);
}

function deepEqual(actual, expected, message = "") {
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

exports = Object.assign(assert, {
  equal, strictEqual, notStrictEqual, ok, deepEqual, doesNotThrow
});
