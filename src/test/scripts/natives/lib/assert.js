/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Assertion failed: got ${actual}, expected ${expected}`);
  }
}
