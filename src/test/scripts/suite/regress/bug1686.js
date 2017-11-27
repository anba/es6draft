/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertFalse
} = Assert;

// 15.17.3: WeakSet.prototype methods need to test input value is Object
// https://bugs.ecmascript.org/show_bug.cgi?id=1686

let ws = new WeakSet();
assertThrows(TypeError, () => ws.add(0));
assertFalse(ws.has(0));
assertFalse(ws.delete(0));
