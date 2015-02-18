/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// Destructuring assignment can call .next() on a spent iterator
// https://bugs.ecmascript.org/show_bug.cgi?id=2997

function iter() {
  var nextCalled = 0;
  var iterator = {
    next() {
      return {value: nextCalled++, done: true};
    },
    [Symbol.iterator]() {
      return this;
    }
  };
  return {iterator, count: () => nextCalled};
}

var {iterator, count} = iter();
var [a, b] = iterator;
assertUndefined(a);
assertUndefined(b);
assertSame(1, count());

var {iterator, count} = iter();
let [c, d] = iterator;
assertUndefined(c);
assertUndefined(d);
assertSame(1, count());

var {iterator, count} = iter();
var e, f; [e, f] = iterator;
assertUndefined(e);
assertUndefined(f);
assertSame(1, count());
