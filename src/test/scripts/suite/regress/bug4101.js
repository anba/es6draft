/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Typo in function name: "get[Symbol.species]" (Regexp, 21.2.4.2)
// https://bugs.ecmascript.org/show_bug.cgi?id=4101

var desc = Object.getOwnPropertyDescriptor(RegExp, Symbol.species);
assertSame("get [Symbol.species]", desc.get.name);
