/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// For consistency, String(symbol) should throw an exception. Use Reflect.* API to retrieve Symbol description
// https://bugs.ecmascript.org/show_bug.cgi?id=3509

assertSame("Symbol()", String(Symbol()));
assertSame("Symbol(abc)", String(Symbol("abc")));
