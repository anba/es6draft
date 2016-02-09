/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertInstanceOf
} = Assert;

// new Object(value), for value != null, is no longer equivalent to ToObject(value)
// https://bugs.ecmascript.org/show_bug.cgi?id=3550

assertInstanceOf(Object, Object());
assertInstanceOf(Object, Object(void 0));
assertInstanceOf(Object, Object(null));

assertInstanceOf(Number, Object(0));
assertInstanceOf(Boolean, Object(true));
assertInstanceOf(String, Object(""));
assertInstanceOf(Symbol, Object(Symbol.iterator));

var o = {};
assertSame(o, Object(o));

assertInstanceOf(Object, new Object());
assertInstanceOf(Object, new Object(void 0));
assertInstanceOf(Object, new Object(null));

assertInstanceOf(Number, new Object(0));
assertInstanceOf(Boolean, new Object(true));
assertInstanceOf(String, new Object(""));
assertInstanceOf(Symbol, new Object(Symbol.iterator));

var o = {};
assertSame(o, new Object(o));
