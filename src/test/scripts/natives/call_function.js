/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

if (typeof assertEq === 'undefined') {
  assertEq = function assertEq(actual, expected) {
    if (actual !== expected) {
      throw new Error(`Assertion failed: got ${actual}, expected ${expected}`);
    }
  }
}

for (let thisArg of [void 0, null, {}]) {
  let callCount = 0;
  %CallFunction(function(){ callCount += 1 }, thisArg);
  assertEq(callCount, 1);
}

for (let thisArg of [void 0, null, {}]) {
  let callCount = 0, sum = 0;
  %CallFunction(function(a, b, c){ callCount += 1; sum = a + b + c }, thisArg, 1, 2, 3);
  assertEq(callCount, 1);
  assertEq(sum, 6);
}

for (let thisArg of [void 0, null, {}]) {
  let callCount = 0;
  %CallFunction(() => { callCount += 1 }, thisArg);
  assertEq(callCount, 1);
}

for (let thisArg of [void 0, null, {}]) {
  let callCount = 0, sum = 0;
  %CallFunction((a, b, c) => { callCount += 1; sum = a + b + c }, thisArg, 1, 2, 3);
  assertEq(callCount, 1);
  assertEq(sum, 6);
}
