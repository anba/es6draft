/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// new early errors should be SyntaxError, not ReferenceError
// https://bugs.ecmascript.org/show_bug.cgi?id=3992

function notEarlyError() {
  fail `Not Early Error`;
}

assertThrows(ReferenceError, () => eval(`notEarlyError(); ++1`));
assertThrows(ReferenceError, () => eval(`notEarlyError(); --1`));
assertThrows(ReferenceError, () => eval(`notEarlyError(); 1++`));
assertThrows(ReferenceError, () => eval(`notEarlyError(); 1--`));
assertThrows(ReferenceError, () => eval(`notEarlyError(); 1 = 0`));
assertThrows(ReferenceError, () => eval(`notEarlyError(); 1 += 0`));
assertThrows(ReferenceError, () => eval(`notEarlyError(); 1 -= 0`));
