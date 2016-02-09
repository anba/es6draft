/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function assertThrowsAt(lineNumber, script) {
  try {
    evalScript(script, {lineNumber: 1, fileName: "error-loc"});
  } catch (e) {
    return assertEq(e.lineNumber, lineNumber);
  }
  assertEq("no exception", null);
}

const globalConst = 0;

Object.defineProperty(this, "nonEnumerable", {
  value: 0, writable: true, enumerable: false, configurable: false
});
Object.defineProperty(this, "nonWritable", {
  value: 0, writable: true, enumerable: false, configurable: false
});

assertThrowsAt(2, ` // line 1
                    function globalConst() {}
`);
assertThrowsAt(2, ` // line 1
                    function nonEnumerable() {}
`);
assertThrowsAt(2, ` // line 1
                    function nonWritable() {}
`);
