/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, fail
} = Assert;

//  21.1.5.1 CreateStringIterator: ToString instead of ToObject
// https://bugs.ecmascript.org/show_bug.cgi?id=2120

let toStringCalled = 0;
let obj = {
  toString() {
    toStringCalled += 1;
    return "abc";
  },
  valueOf() {
    fail `called valueOf() instead of toString()`;
  }
};

let iter = String.prototype[Symbol.iterator].call(obj);
assertSame(1, toStringCalled);

[...iter]; // drain iterator
assertSame(1, toStringCalled);
