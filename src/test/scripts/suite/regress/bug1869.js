/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// Implicit ToObject() conversion in destructuring still present for some productions
// https://bugs.ecmascript.org/show_bug.cgi?id=1869

assertThrows(() => { var [head, ...tail] = "123" }, TypeError);
assertThrows(() => { for (var [head, ...tail] of "123") ; }, TypeError);
