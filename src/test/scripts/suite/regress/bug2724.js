/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 9.4.4.7 CreateMappedArgumentsObject: Add func as .callee property
// https://bugs.ecmascript.org/show_bug.cgi?id=2724

let f = function(){ return arguments }
let args = f();
assertDataProperty(args, "callee", {value: f, writable: true, enumerable: false, configurable: true});
