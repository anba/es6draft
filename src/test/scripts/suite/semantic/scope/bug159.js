/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertThrows,
} = Assert;

// Test cases from https://bugs.ecmascript.org/show_bug.cgi?id=159
// assertion in 10.2.1.1.3 step 2 is incorrect


function testAssignment() {
  var log = [];
  function p(v) { log.push(v); }
  var x = 2;
  p(x);
  (function() {
    p(x);
    eval("var x = 5");
    p(x);
    x = eval("p(x); p(delete x); p(x); x + 1");
    p(x);
    p(delete x);
    p(x);
    p(delete x);
    p(x);
  })();
  p(x);
  assertEquals([2, 2, 5, 5, true, 2, 3, true, 2, false, 2, 2], log);
}
testAssignment();

function testCompoundAssignment() {
  var log = [];
  function p(v) { log.push(v); }
  var x = 2;
  p(x);
  (function() {
    p(x);
    eval("var x = 5");
    p(x);
    x += eval("p(x); p(delete x); p(x); x");
    p(x);
    p(delete x);
    p(x);
    p(delete x);
    p(x);
  })();
  p(x);
  assertEquals([2, 2, 5, 5, true, 2, 7, true, 2, false, 2, 2], log);
}
testCompoundAssignment();
