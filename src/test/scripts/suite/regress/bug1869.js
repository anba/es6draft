/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals, assertThrows
} = Assert;

// Implicit ToObject() conversion in destructuring still present for some productions
// https://bugs.ecmascript.org/show_bug.cgi?id=1869

var [head, ...tail] = "123";
assertSame("1", head);
assertEquals(["2", "3"], tail);

for (var [head, ...tail] of "123") ;
assertThrows(TypeError, () => { for (var [head, ...tail] of void 0) ; });
assertThrows(TypeError, () => { for (var [head, ...tail] of null) ; });
