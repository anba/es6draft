/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Make class name TDZ in extends expression
// https://bugs.ecmascript.org/show_bug.cgi?id=3306

var B = class {};
assertThrows(ReferenceError, () => class B extends B {});
