/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 13.1.1.2, Runtime Semantics: Binding Initialisation: No longer possible to use standard Indexed Binding Initialisation
// https://bugs.ecmascript.org/show_bug.cgi?id=1866

// no error
function f(a) {}
f();

// no prototype lookup
Object.prototype[0] = "xxx";
function g(a) { return a }
assertUndefined(g());
