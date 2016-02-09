/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertDataProperty
} = Assert;

// @@toStringTag protected builtins list not complete?
// https://bugs.ecmascript.org/show_bug.cgi?id=3506

assertDataProperty(JSON, Symbol.toStringTag, {value: "JSON", writable: false, enumerable: false, configurable: true});
assertDataProperty(Math, Symbol.toStringTag, {value: "Math", writable: false, enumerable: false, configurable: true});

assertSame("[object JSON]", {[Symbol.toStringTag]: "JSON"}.toString());
assertSame("[object Math]", {[Symbol.toStringTag]: "Math"}.toString());
