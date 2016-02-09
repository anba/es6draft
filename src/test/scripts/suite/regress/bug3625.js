/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.6.4.12 ForIn/OfExpressionEvaluation: Remove ToObject in step 8b
// https://bugs.ecmascript.org/show_bug.cgi?id=3625

var callCount = 0;
var callThis;

Object.defineProperty(Number.prototype, Symbol.iterator, {
  get() {
    "use strict";
    return () => {
      callCount += 1;
      callThis = this;
      return function* (from, to) {
        for (let i = from; i <= to; ++i) yield i;
      }(0, Math.max(0, this | 0));
    };
  }
});

var sum = 0;
for (let n of 10) {
  sum += n;
}

assertSame(1, callCount);
assertSame(10, callThis);
assertSame(55, sum);
