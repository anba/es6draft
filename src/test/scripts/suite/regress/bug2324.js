/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 9.2.10: No get/set prefix for accessor property for empty string
// https://bugs.ecmascript.org/show_bug.cgi?id=2324

assertSame("get ", Object.getOwnPropertyDescriptor({get ""(){}}, "").get.name);
assertSame("set ", Object.getOwnPropertyDescriptor({set ""(v){}}, "").set.name);
