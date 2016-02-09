/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 12.1.1 Early Errors: "yield" with escape sequences not handled
// https://bugs.ecmascript.org/show_bug.cgi?id=3354

assertSyntaxError(`function*g() { var y\u0069eld }`)
assertSyntaxError(String.raw `function*g() { var y\u0069eld }`)
