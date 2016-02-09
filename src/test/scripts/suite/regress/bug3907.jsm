/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 7.4.8 CreateListIterator, 7.4.9 CreateCompoundIterator: Change "next" method to non-enumerable?
// https://bugs.ecmascript.org/show_bug.cgi?id=3907

import* as self from "./bug3907.jsm";

var iter = Reflect.enumerate(self);

assertDataProperty(iter, "next", {
  value: iter.next,
  writable: true, enumerable: false, configurable: true
});
