/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.4.4.2 doesn't reflect reality
// https://bugs.ecmascript.org/show_bug.cgi?id=4371

function argumentsNonConfigurableThenNonWritableWithInterveningSetMutableBinding(a) {
  Object.defineProperty(arguments, "0", {configurable: false});
  a = 2;
  Object.defineProperty(arguments, "0", {writable: false});
  assertSame(2, a);
  // assertSame(1, arguments[0]);
  assertSame(2, arguments[0]);

  // Postcondition: Arguments mapping is removed.
  a = 3;
  assertSame(3, a);
  // assertSame(1, arguments[0]);
  assertSame(2, arguments[0]);
}
argumentsNonConfigurableThenNonWritableWithInterveningSetMutableBinding(1);
