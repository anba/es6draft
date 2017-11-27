/*
 * Copyright (c) André Bargull
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


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values triggers a ReferenceError in non-strict mode
function testForLoop() {
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertThrows(ReferenceError, testForLoop);


// Lexical environment of ForStatement is created before loop-entry, any reference
// within destructuring default values triggers a ReferenceError in strict mode
function strictTestForLoop() {
  "use strict";
  let x = 0;
  for (let {x = x} = {};;) return x;
}
assertThrows(ReferenceError, strictTestForLoop);


// Lexical environment of ForOfStatement is created before loop-entry, any reference
// within destructuring default values triggers a ReferenceError in strict mode
function testForOfLoop() {
  let x = 0;
  for (let {x = x} of [{}]) return x;
}
assertThrows(ReferenceError, testForOfLoop);
