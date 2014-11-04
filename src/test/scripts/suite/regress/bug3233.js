/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 9.1.11 [[Enumerate]]: Explicitly mention %IteratorPrototype% is the prototype?
// https://bugs.ecmascript.org/show_bug.cgi?id=3233

var StringIteratorPrototype = Object.getPrototypeOf(""[Symbol.iterator]());
var IteratorPrototype = Object.getPrototypeOf(StringIteratorPrototype);

assertSame(Object.prototype, Object.getPrototypeOf(IteratorPrototype));
assertSame(IteratorPrototype, Object.getPrototypeOf(Reflect.enumerate({})));
