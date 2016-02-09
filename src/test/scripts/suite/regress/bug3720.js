/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 9.4.2.4 ArraySetLength: Missing ReturnIfAbrupt in steps 3 & 4
// https://bugs.ecmascript.org/show_bug.cgi?id=3720

class Err extends Error { }

var thrower = {
  valueOf() { throw new Err() }
};
var thrower2 = {
  calledOnce: false,
  valueOf() {
    if (this.calledOnce) {
      throw new Err();
    }
    this.calledOnce = true;
  }
};

assertThrows(Err, () => Object.defineProperty([], "length", {value: thrower}));
assertThrows(Err, () => Object.defineProperty([], "length", {value: thrower2}));
