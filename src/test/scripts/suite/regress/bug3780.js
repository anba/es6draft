/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

// 21.1.3.17 String.prototype.split: Move ToString to top ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3780

var thrower = {
  toString() {
    fail `toString called`;
  },
  valueOf() {
    fail `valueOf called`;
  },
};

var regExpLike = {
  [Symbol.match]() {
    return "match";
  },
  [Symbol.search]() {
    return "search";
  },
  [Symbol.split]() {
    return "split";
  },
  [Symbol.replace]() {
    return "replace";
  },
};

assertSame("match", String.prototype.match.call(thrower, regExpLike));
assertSame("search", String.prototype.search.call(thrower, regExpLike));
assertSame("split", String.prototype.split.call(thrower, regExpLike));
assertSame("replace", String.prototype.replace.call(thrower, regExpLike));
