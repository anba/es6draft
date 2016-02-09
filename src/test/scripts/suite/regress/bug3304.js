/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// name property of anonymous class expression?
// https://bugs.ecmascript.org/show_bug.cgi?id=3304

var C = class {};
assertTrue(C.hasOwnProperty('name'));
assertSame("C", C.name);
