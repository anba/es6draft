/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 15.3.4.7: writable+configurable Function.prototype[@@hasInstance] can reveal [[BoundTarget]]
// https://bugs.ecmascript.org/show_bug.cgi?id=1446
// https://bugs.ecmascript.org/show_bug.cgi?id=1873

assertDataProperty(Function.prototype, Symbol.hasInstance, {
  value: Function.prototype[Symbol.hasInstance],
  writable: false, enumerable: false, configurable: false
});
