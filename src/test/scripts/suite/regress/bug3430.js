/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Function.prototype.toMethod should check type-check 'this' before arguments
// https://bugs.ecmascript.org/show_bug.cgi?id=3430

assertThrows(TypeError, () => Function.prototype.toMethod.call({}, null));
assertThrows(TypeError, () => Function.prototype.toMethod.call({}, {}));
