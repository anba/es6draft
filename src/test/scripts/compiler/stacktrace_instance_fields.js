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

var o = new class Base {
  st1 = new Error().stackTrace;

  st2 = new Error().stackTrace;
}

var st1 = o.st1;
var st2 = o.st2;

assertEq(st1.length, 2);
assertEq(st1[0].methodName, "Base");
assertEq(st1[0].lineNumber, 14);
assertEq(st1[1].methodName, "");
assertEq(st1[1].lineNumber, 13);

assertEq(st2.length, 2);
assertEq(st2[0].methodName, "Base");
assertEq(st2[0].lineNumber, 16);
assertEq(st2[1].methodName, "");
assertEq(st2[1].lineNumber, 13);

var o = new class Derived extends class Base {} {
  st1 = new Error().stackTrace;

  st2 = new Error().stackTrace;
}

var st1 = o.st1;
var st2 = o.st2;

assertEq(st1.length, 2);
assertEq(st1[0].methodName, "Derived");
assertEq(st1[0].lineNumber, 35);
assertEq(st1[1].methodName, "");
assertEq(st1[1].lineNumber, 34);

assertEq(st2.length, 2);
assertEq(st2[0].methodName, "Derived");
assertEq(st2[0].lineNumber, 37);
assertEq(st2[1].methodName, "");
assertEq(st2[1].lineNumber, 34);
