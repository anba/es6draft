/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertNotSame, assertUndefined, assertNotUndefined,
} = Assert;

// Add `Symbol.iterator` on the %IteratorPrototype% that returns `this`
// https://bugs.ecmascript.org/show_bug.cgi?id=3254

var iterators = [
  Object.getPrototypeOf(""[Symbol.iterator]()),
  Object.getPrototypeOf([][Symbol.iterator]()),
  Object.getPrototypeOf((new Map)[Symbol.iterator]()),
  Object.getPrototypeOf((new Set)[Symbol.iterator]()),
  Reflect.enumerate({}),
  Object.getPrototypeOf(function*(){}.prototype),
];

var IteratorPrototype = Object.getPrototypeOf(iterators[0]);
assertNotUndefined(Object.getOwnPropertyDescriptor(IteratorPrototype, Symbol.iterator));

for (var iter of iterators) {
  assertSame(IteratorPrototype, Object.getPrototypeOf(iter));
  assertUndefined(Object.getOwnPropertyDescriptor(iter, Symbol.iterator));
}
