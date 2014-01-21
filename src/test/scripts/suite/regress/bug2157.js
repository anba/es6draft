/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows, fail
} = Assert;

// 8.3.16.10: Invalid assertion in step 1
// https://bugs.ecmascript.org/show_bug.cgi?id=2157

let stringIterator = String.prototype[Symbol.iterator];

assertThrows(() => stringIterator.call(void 0), TypeError);
assertThrows(() => stringIterator.call(null), TypeError);

let toStringCalled = 0;
stringIterator.call({
  toString() {
    toStringCalled += 1;
    return "";
  },
  valueOf() {
    fail `called valueOf() instead of toString()`;
  }
});
assertSame(1, toStringCalled);
