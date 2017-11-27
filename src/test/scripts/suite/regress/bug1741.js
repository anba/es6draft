/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.2.3.4, 15.2.3.14: Object.getOwnPropertyNames() and Object.keys() return wrong list
// https://bugs.ecmascript.org/show_bug.cgi?id=1741

assertSame(0, Object.getOwnPropertyNames({[Symbol()]: null}).length);
assertSame(0, Object.keys({[Symbol()]: null}).length);
