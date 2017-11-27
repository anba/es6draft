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
  #p1 = new Error().stackTrace;

  #p2 = new Error().stackTrace;

  st1() { return this.#p1; }
  st2() { return this.#p2; }
}

var st1 = o.st1();
var st2 = o.st2();

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
  #p1 = new Error().stackTrace;

  #p2 = new Error().stackTrace;

  st1() { return this.#p1; }
  st2() { return this.#p2; }
}

var st1 = o.st1();
var st2 = o.st2();

assertEq(st1.length, 2);
assertEq(st1[0].methodName, "Derived");
assertEq(st1[0].lineNumber, 38);
assertEq(st1[1].methodName, "");
assertEq(st1[1].lineNumber, 37);

assertEq(st2.length, 2);
assertEq(st2[0].methodName, "Derived");
assertEq(st2[0].lineNumber, 40);
assertEq(st2[1].methodName, "");
assertEq(st2[1].lineNumber, 37);
