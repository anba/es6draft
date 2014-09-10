/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 19.4.3.2 Symbol.prototype.toString ( ), 19.4.3.4 Symbol.prototype.valueOf ( ): Add type checks for Object type
// https://bugs.ecmascript.org/show_bug.cgi?id=3187

for (let v of [void 0, null, true, false, 0, 1, 1.5, 0/0, 1/0, -1/0]) {
  assertThrows(TypeError, () => Symbol.prototype.toString.call(v));
  assertThrows(TypeError, () => Symbol.prototype.valueOf.call(v));
}
