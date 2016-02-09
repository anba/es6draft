/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Reflect.construct should compare with undefined
// https://bugs.ecmascript.org/show_bug.cgi?id=4416

Reflect.construct(Object, []);
assertThrows(TypeError, () => Reflect.construct(Object, [], void 0));
