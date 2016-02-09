/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// 25.3.2.1, 25.3.2.2: configurable mixed up for GeneratorFunction.length and GeneratorFunction.prototype
// https://bugs.ecmascript.org/show_bug.cgi?id=1927

let GeneratorFunction = (function*(){}).constructor;
assertTrue(Object.getOwnPropertyDescriptor(GeneratorFunction, "length").configurable);
assertFalse(Object.getOwnPropertyDescriptor(GeneratorFunction, "prototype").configurable);
