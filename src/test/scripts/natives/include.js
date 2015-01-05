/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
%Include("lib/assert.js");

// Top level include
try {
  %Include("lib/throw.js");
  throw new Error("no error");
} catch (e) {
  assertEq(e.message, "expected");
}

(function functionContext() {
  try {
    %Include("lib/throw.js");
    throw new Error("no error");
  } catch (e) {
    assertEq(e.message, "expected");
  }
}());


// Test include relative from "lib/includeFrom.js"
%Include("lib/includeFrom.js");

try {
  includeFrom("./throw.js");
  throw new Error("no error");
} catch (e) {
  assertEq(e.message, "expected");
}

(function functionContext() {
  try {
    includeFrom("./throw.js");
    throw new Error("no error");
  } catch (e) {
    assertEq(e.message, "expected");
  }
}());
