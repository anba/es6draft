/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// [21.2.5.11 RegExp.prototype [ @@split ]: Missing Boundary Check
// https://bugs.ecmascript.org/show_bug.cgi?id=4434

var regExp = {
  constructor: {
    [Symbol.species]: function() {
      return regExp;
    }
  },
  exec() {
    this.lastIndex = 10;
    return {
      length: 0,
      index: 0,
    };
  }
};

RegExp.prototype[Symbol.split].call(regExp, "foo");
