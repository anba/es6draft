/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals, assertThrows
} = Assert;


// Lexical environment of Catch is activated before binding initialisation, any reference
// within destructuring default values triggers a ReferenceError in non-strict mode
function testCatch() {
  var x = 0;
  try {
    throw {};
  } catch({x = x}) {
    return x;
  }
}
assertThrows(ReferenceError, testCatch);

// Test with nested lexical environment introduced by ArrayComprehension
function testCatchWithArrayComprehension() {
  var x = [0];
  try {
    throw {};
  } catch({x = [1], e = [for (x of x) x + 1]}) {
    return e;
  }
}
assertEquals([2], testCatchWithArrayComprehension());


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values triggers a ReferenceError in non-strict mode
function testForLoop() {
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertThrows(ReferenceError, testForLoop);

// Test with nested lexical environment introduced by ArrayComprehension
function testForLoopWithArrayComprehension() {
  let x = [0];
  for (let {x = [for (x of x) x + 1]} = {};;) return x;
}
assertThrows(ReferenceError, testForLoopWithArrayComprehension);


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values triggers a ReferenceError in strict mode
function strictTestForLoop() {
  "use strict";
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertThrows(ReferenceError, strictTestForLoop);

// Test with nested lexical environment introduced by ArrayComprehension
function strictTestForLoopWithArrayComprehension() {
  "use strict";
  let x = [0];
  for (let {x = [for (x of x) x + 1]} = {};;) return x;
}
assertThrows(ReferenceError, strictTestForLoopWithArrayComprehension);


// Lexical environment of ForOfStatement is created before loop-entry, any reference
// within destructuring default values triggers a ReferenceError in strict mode
function testForOfLoop() {
  let x = 0;
  for (let {x = x} of [{}]) return x;
}
assertThrows(ReferenceError, testForOfLoop);

// Test with nested lexical environment introduced by ArrayComprehension
function testForOfLoopWithArrayComprehension() {
  let x = [0];
  for (let {x = [for (x of x) x + 1]} of [{}]) return x;
}
assertThrows(ReferenceError, testForOfLoopWithArrayComprehension);
