/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 23.1.5.1 CreateMapIterator: Missing initialisation check
// https://bugs.ecmascript.org/show_bug.cgi?id=2396

assertThrows(() => Map[Symbol.create]().entries(), TypeError);
assertThrows(() => Map[Symbol.create]().keys(), TypeError);
assertThrows(() => Map[Symbol.create]().values(), TypeError);
assertThrows(() => Map[Symbol.create]()[Symbol.iterator](), TypeError);

assertThrows(() => Set[Symbol.create]().entries(), TypeError);
assertThrows(() => Set[Symbol.create]().keys(), TypeError);
assertThrows(() => Set[Symbol.create]().values(), TypeError);
assertThrows(() => Set[Symbol.create]()[Symbol.iterator](), TypeError);
