/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No ReferenceError
function f() {
  let x = 0;
  switch (0) {
    case 1: let x = 1;
  }
  return x;
}
f();
