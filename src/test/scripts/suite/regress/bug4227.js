/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotUndefined
} = Assert;

// 23.1.2.2, 23.2.2.2: @@species for Map and Set required ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4227

assertNotUndefined(Object.getOwnPropertyDescriptor(Map, Symbol.species));
assertNotUndefined(Map[Symbol.species]);

assertNotUndefined(Object.getOwnPropertyDescriptor(Set, Symbol.species));
assertNotUndefined(Set[Symbol.species]);
