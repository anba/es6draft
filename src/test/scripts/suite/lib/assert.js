/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Assert(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      SyntaxError = global.SyntaxError;

const Object_is = Object.is,
      Object_prototype_toString = Object.prototype.toString;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

function safeToString(o) {
  try {
    return "" + o;
  } catch (e) {
    try {
      return $CallFunction(Object_prototype_toString, o);
    } catch (e) {
      return "???";
    }
  }
}

function fmt(callSite, ...substitutions) {
  let cooked = Object(callSite);
  let literalSegments = cooked.length >>> 0;
  if (literalSegments === 0) {
    return "";
  }
  let r = "";
  for (let nextIndex = 0;; ++nextIndex) {
    r += cooked[nextIndex];
    if (nextIndex + 1 === literalSegments) {
      return r;
    }
    r += safeToString(substitutions[nextIndex]);
  }
}

class AssertionError extends Error {
  get name() {
    return "AssertionError";
  }
}

function fail(format = "", ...args) {
  let message = typeof format === "string" ? format : fmt(format, ...args);
  throw new AssertionError(message);
}

function assertSame(expected, actual) {
  if (!Object_is(expected, actual)) {
    fail `Expected «${expected}», but got «${actual}»`;
  }
}

function assertThrows(f, expected) {
  try {
    f();
  } catch (e) {
    if (e instanceof expected) {
      return;
    }
    fail `Expected error «${expected.name}», but got «${e}»`;
  }
  fail `Expected error «${expected.name}»`;
}

function assertTrue(actual) {
  return assertSame(true, actual);
}

function assertFalse(actual) {
  return assertSame(false, actual);
}

function assertSyntaxError(code) {
  return assertThrows(() => Function(code), SyntaxError);
}

// export...
Object.defineProperty(global, "Assert", {value: {
  fail, assertSame, assertThrows, assertTrue, assertFalse, assertSyntaxError
}});

})(this);
