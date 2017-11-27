/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.2.2.1 DataView: Remove length integer validation and use ToLength ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4005

assertThrows(RangeError, () => new DataView(new ArrayBuffer(0), 0, -1));
