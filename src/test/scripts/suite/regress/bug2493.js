/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotSame, assertTrue, assertFalse, 
} = Assert;

// 22.2.1.4: Remove step 16
// https://bugs.ecmascript.org/show_bug.cgi?id=2493

// no error
new Int8Array(new ArrayBuffer(0), 0, 0);
