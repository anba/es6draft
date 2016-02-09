/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined, assertCallable, assertSame
} = Assert;

// %ThrowTypeError% has a mutable name property
// https://bugs.ecmascript.org/show_bug.cgi?id=4267

var descCaller = Object.getOwnPropertyDescriptor(Function.prototype, "caller");
var descArguments = Object.getOwnPropertyDescriptor(Function.prototype, "arguments");
var thrower = descCaller.get;

assertCallable(thrower);
assertSame(thrower, descCaller.set);
assertSame(thrower, descArguments.get);
assertSame(thrower, descArguments.set);
assertUndefined(Object.getOwnPropertyDescriptor(thrower, "name"));
assertSame("", thrower.name);
