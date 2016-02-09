/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Error behaviour for non-generic functions should be described in more detail
// https://bugs.ecmascript.org/show_bug.cgi?id=1123

assertThrows(TypeError, () => Number.prototype.toFixed.call({}, {valueOf() { throw new Error }}));
