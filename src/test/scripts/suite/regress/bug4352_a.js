/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

// Annex E: Changed evaluation order in for-in statement
// https://bugs.ecmascript.org/show_bug.cgi?id=4352

var o = {a: 0};
var p = {
  get q() {
    delete o.a;
    return {};
  }
};
var loopEntered = false;
for (p.q.v in o) {
  loopEntered = true;
}
assertTrue(loopEntered);
