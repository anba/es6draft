/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

const legacyArguments = function f() { return f.arguments; }();

// No crash
Object.setPrototypeOf(Object.getPrototypeOf(Reflect.enumerate({})), legacyArguments);
for (var k in {a: 0, b: 0}) break;
