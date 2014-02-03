/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals, assertThrows, assertUndefined
} = Assert;


// Lexical environment of Catch is activated after binding initialisation, any reference
// within destructuring default values refers to the surrounding environment
function testCatch() {
  var x = 0;
  try {
    throw {};
  } catch({x = x}) {
    return x;
  }
}
assertSame(0, testCatch());

// Test with nested lexical environment introduced by ArrayComprehension
function testCatchWithArrayComprehension() {
  var x = [0];
  try {
    throw {};
  } catch({x = [1], e = [for (x of x) x + 1]}) {
    return e;
  }
}
assertEquals([1], testCatchWithArrayComprehension());


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values returns undefined in non-strict mode
function testForLoop() {
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertUndefined(testForLoop());

// Test with nested lexical environment introduced by ArrayComprehension
function testForLoopWithArrayComprehension() {
  let x = [0];
  for (let {x = [for (x of x) x + 1]} = {};;) return x;
}
assertThrows(testForLoopWithArrayComprehension, TypeError);


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values trigger a ReferenceError in strict mode
function strictTestForLoop() {
  "use strict";
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertThrows(strictTestForLoop, ReferenceError);

// Test with nested lexical environment introduced by ArrayComprehension
function strictTestForLoopWithArrayComprehension() {
  "use strict";
  let x = [0];
  for (let {x = [for (x of x) x + 1]} = {};;) return x;
}
assertThrows(strictTestForLoopWithArrayComprehension, ReferenceError);


// Lexical environment of ForOfStatement is created after loop-entry, any reference
// within destructuring default values refers to the surrounding environment
function testForOfLoop() {
  let x = 0;
  for (let {x = x} of [{}]) return x;
}
assertSame(0, testForOfLoop());

// Test with nested lexical environment introduced by ArrayComprehension
function testForOfLoopWithArrayComprehension() {
  let x = [0];
  for (let {x = [for (x of x) x + 1]} of [{}]) return x;
}
assertEquals([1], testForOfLoopWithArrayComprehension());
