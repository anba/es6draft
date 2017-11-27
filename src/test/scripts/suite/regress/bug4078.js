/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertEquals
} = Assert;

// 21.2 RegExp: Incomplete compatibility for %RegExpPrototype%
// https://bugs.ecmascript.org/show_bug.cgi?id=4078

assertThrows(TypeError, () => "".match(RegExp.prototype));
assertThrows(TypeError, () => "".replace(RegExp.prototype));
assertThrows(TypeError, () => "".search(RegExp.prototype));
assertEquals([], "".split(RegExp.prototype));

assertThrows(TypeError, () => RegExp.prototype.exec(""));
assertThrows(TypeError, () => RegExp.prototype.test(""));
assertThrows(TypeError, () => RegExp.prototype[Symbol.match](""));
assertThrows(TypeError, () => RegExp.prototype[Symbol.replace](""));
assertThrows(TypeError, () => RegExp.prototype[Symbol.search](""));
assertEquals([], RegExp.prototype[Symbol.split](""));
