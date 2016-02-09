/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Annex E: Document that NativeError constructors inherit from %Error%
// https://bugs.ecmascript.org/show_bug.cgi?id=3514

assertSame(Error, Object.getPrototypeOf(EvalError));
assertSame(Error, Object.getPrototypeOf(RangeError));
assertSame(Error, Object.getPrototypeOf(ReferenceError));
assertSame(Error, Object.getPrototypeOf(SyntaxError));
assertSame(Error, Object.getPrototypeOf(TypeError));
assertSame(Error, Object.getPrototypeOf(URIError));
