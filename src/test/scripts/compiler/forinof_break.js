/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
function test() {
  for (let a of x) {
    for (let b of y) {
      break;
    }
  }
}
