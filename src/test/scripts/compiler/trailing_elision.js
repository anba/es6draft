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

var n = eval(`
  [ ${"0,".repeat(1000)} 1, ${",".repeat(5)} ].length;
`);
assertEq(n, 1000 + 1 + 5);
