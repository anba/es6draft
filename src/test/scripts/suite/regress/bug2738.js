/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertDataProperty
} = Assert;

// 14.5.17 ClassDefinitionEvaluation: Missing [[Value]] field in step 12
// https://bugs.ecmascript.org/show_bug.cgi?id=2738

class C { }
assertSame(C, C.prototype.constructor);
assertDataProperty(C.prototype, "constructor", {value: C, writable: true, enumerable: false, configurable: true});
