/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Replace definitions from harness/sta.js

var NotEarlyError = new Error("NotEarlyError");

class Test262Error extends Error {
  get name() {
    return "Test262Error";
  }
}

function $ERROR(message) {
  throw new Test262Error(message);
}
