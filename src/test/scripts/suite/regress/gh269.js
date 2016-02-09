/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertEquals
} = Assert;

// Don't recurse in typed array constructor
// https://github.com/tc39/ecma262/pull/269

let alreadyCalled = false;
let callArgs;

class MyInt8 extends Int8Array {
  constructor(...args) {
    assertFalse(alreadyCalled);
    alreadyCalled = true;
    callArgs = args;
    super(...args);
  }
}

assertFalse(alreadyCalled);
new MyInt8([1, 2, 3]);
assertTrue(alreadyCalled);
assertEquals([[1, 2, 3]], callArgs);
