/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Accessing `RegExp.prototype.global` etc. directly must not throw to match existing implementations
// https://bugs.ecmascript.org/show_bug.cgi?id=3432

assertThrows(TypeError, () => RegExp.prototype.global);
assertThrows(TypeError, () => RegExp.prototype.ignoreCase);
assertThrows(TypeError, () => RegExp.prototype.multiline);
assertThrows(TypeError, () => RegExp.prototype.sticky);
assertThrows(TypeError, () => RegExp.prototype.unicode);
