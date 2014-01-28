/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals, assertThrows
} = Assert;


// Lexical environment of Catch is activated after binding initialisation, any reference
// within destructuring default values refers to the surrounding environment
function testCatch() {
  "use strict";
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
  "use strict";
  var x = [0];
  try {
    throw {};
  } catch({x = [1], e = [for (x of x) x + 1]}) {
    return e;
  }
}
assertEquals([1], testCatchWithArrayComprehension());


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values trigger a ReferenceError in strict mode
function testForLoop() {
  "use strict";
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertThrows(testForLoop, ReferenceError);

// Test with nested lexical environment introduced by ArrayComprehension
function testForLoopWithArrayComprehension() {
  "use strict";
  let x = [0];
  for (let {x = [for (x of x) x + 1]} = {};;) return x;
}
assertThrows(testForLoopWithArrayComprehension, ReferenceError);


// Lexical environment of ForOfStatement is created after loop-entry, any reference
// within destructuring default values refers to the surrounding environment
function testForOfLoop() {
  "use strict";
  let x = 0;
  for (let {x = x} of [{}]) return x;
}
assertSame(0, testForOfLoop());

// Test with nested lexical environment introduced by ArrayComprehension
function testForOfLoopWithArrayComprehension() {
  "use strict";
  let x = [0];
  for (let {x = [for (x of x) x + 1]} of [{}]) return x;
}
assertEquals([1], testForOfLoopWithArrayComprehension());
