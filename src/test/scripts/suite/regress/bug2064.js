/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertAccessorProperty
} = Assert;

// 9.2.6.1 CreateBuiltinFunction: Add call to AddRestrictedFunctionProperties?
// https://bugs.ecmascript.org/show_bug.cgi?id=2064

const ThrowTypeError = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;

let builtin = Object.create;
assertAccessorProperty(builtin, "caller", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: true});
assertAccessorProperty(builtin, "arguments", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: true});
