/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 19.2.4 Function Instances: Strict and built-in functions no longer have "caller" and "arguments" own properties
// https://bugs.ecmascript.org/show_bug.cgi?id=3185

assertUndefined(Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller"));
assertUndefined(Object.getOwnPropertyDescriptor(function(){"use strict"}, "arguments"));

assertUndefined(Object.getOwnPropertyDescriptor(Object.create, "caller"));
assertUndefined(Object.getOwnPropertyDescriptor(Object.create, "arguments"));
