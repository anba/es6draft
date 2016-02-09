/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 9.2.6.1 CreateBuiltinFunction: Add call to AddRestrictedFunctionProperties?
// https://bugs.ecmascript.org/show_bug.cgi?id=2064

let builtin = Object.create;
assertUndefined(Object.getOwnPropertyDescriptor(builtin, "caller"));
assertUndefined(Object.getOwnPropertyDescriptor(builtin, "arguments"));
