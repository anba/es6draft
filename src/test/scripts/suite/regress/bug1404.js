/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertDataProperty
} = Assert;

// 15.11.1.1: "message" property no longer non-enumerable
// https://bugs.ecmascript.org/show_bug.cgi?id=1404

const constructors = [
  Error, EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError
];
for (let ctor of constructors) {
  let message = "should-be-non-enumerable";
  assertDataProperty(new ctor(message), "message", {value: message, writable: true, enumerable: false, configurable: true});
}
