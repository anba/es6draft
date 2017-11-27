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

var loc = Reflect.parse("\n   a=>{}").body[0].expression.loc;
assertEq(loc.start.line, 2);
assertEq(loc.start.column, 3);
assertEq(loc.end.line, 2);
assertEq(loc.end.column, 8);

var loc = Reflect.parse("   (\n)=>{}").body[0].expression.loc;
assertEq(loc.start.line, 1);
assertEq(loc.start.column, 3);
assertEq(loc.end.line, 2);
assertEq(loc.end.column, 5);
