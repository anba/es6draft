/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 15.15.5.2 WeakMap.prototype.clear is a significant extension without consensus
// https://bugs.ecmascript.org/show_bug.cgi?id=1232

assertUndefined(WeakMap.prototype.clear)
assertUndefined(WeakSet.prototype.clear)
