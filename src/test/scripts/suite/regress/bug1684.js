/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 15.14.1.1, 15.15.1.1: Missing ToObject() calls in {Weak}Map constructor
// https://bugs.ecmascript.org/show_bug.cgi?id=1684

assertThrows(TypeError, () => new Map([0]));
assertThrows(TypeError, () => new WeakMap([0]));
