/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.2.2.1, DataView: Ensure [[ArrayBufferData]] is not undefined in step 6
// https://bugs.ecmascript.org/show_bug.cgi?id=2050

assertThrows(TypeError, () => new DataView(ArrayBuffer[Symbol.create]()));
