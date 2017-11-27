/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function stackTrace() {
  var st = new Error().stackTrace;
  return {value: () => st};
}

var o = {
  @stackTrace
  m1() {},

  @stackTrace
  m2() {},
};

var st1 = o.m1();
var st2 = o.m2();

assertEq(st1.length, 2);
assertEq(st1[0].methodName, "stackTrace");
assertEq(st1[0].lineNumber, 14);
assertEq(st1[1].methodName, "");
assertEq(st1[1].lineNumber, 19);

assertEq(st2.length, 2);
assertEq(st2[0].methodName, "stackTrace");
assertEq(st2[0].lineNumber, 14);
assertEq(st2[1].methodName, "");
assertEq(st2[1].lineNumber, 22);

var o = new class {
  @stackTrace
  m1() {}

  @stackTrace
  m2() {}
};

var st1 = o.m1();
var st2 = o.m2();

assertEq(st1.length, 2);
assertEq(st1[0].methodName, "stackTrace");
assertEq(st1[0].lineNumber, 14);
assertEq(st1[1].methodName, "");
assertEq(st1[1].lineNumber, 42);

assertEq(st2.length, 2);
assertEq(st2[0].methodName, "stackTrace");
assertEq(st2[0].lineNumber, 14);
assertEq(st2[1].methodName, "");
assertEq(st2[1].lineNumber, 45);
