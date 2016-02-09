/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

// 9.4.3, 9.4.5: Missing [[Has]] implementations
// https://bugs.ecmascript.org/show_bug.cgi?id=3511

assertTrue("0" in new String("abc"));
assertTrue("0" in new Int8Array(1));
