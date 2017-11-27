/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

assertSyntaxError(`function* g() { class C { #private = yield; } }`);
assertSyntaxError(`function* g() { class C { #private = yield 0; } }`);
