/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 23.1.5.1 CreateMapIterator: Missing initialisation check
// https://bugs.ecmascript.org/show_bug.cgi?id=2396

assertThrows(TypeError, () => Map[Symbol.create]().entries());
assertThrows(TypeError, () => Map[Symbol.create]().keys());
assertThrows(TypeError, () => Map[Symbol.create]().values());
assertThrows(TypeError, () => Map[Symbol.create]()[Symbol.iterator]());

assertThrows(TypeError, () => Set[Symbol.create]().entries());
assertThrows(TypeError, () => Set[Symbol.create]().keys());
assertThrows(TypeError, () => Set[Symbol.create]().values());
assertThrows(TypeError, () => Set[Symbol.create]()[Symbol.iterator]());
