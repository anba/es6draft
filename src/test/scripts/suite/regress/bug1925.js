/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 21.2.3.3 - RegExpAlloc: Invalid assertion in step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=1925

assertThrows(TypeError, () => RegExp[Symbol.create].call({}));
