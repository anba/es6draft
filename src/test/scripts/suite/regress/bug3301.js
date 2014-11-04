/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertFalse
} = Assert;

// Globals not treated as var bindings
// https://bugs.ecmascript.org/show_bug.cgi?id=3301

evalScript("function f(x) { return x === undefined }");
evalScript("let undefined = 666;");

// FIXME: Update after spec bug was fixed
assertSame(666, undefined);
assertFalse(f());
