/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 9.4.3.1 [[GetOwnProperty]] ( P ): Non-integer indices not handled
// https://bugs.ecmascript.org/show_bug.cgi?id=2761

assertUndefined(Object.getOwnPropertyDescriptor("string", 1.4));
