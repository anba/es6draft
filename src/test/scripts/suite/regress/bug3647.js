/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotNull
} = Assert;

// 21.2.5.6 RegExp.prototype [ @@match ]: Call ToString on match before CreateDataProperty ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3647

const matchResult = {
  toString() {
    return "";
  }
};

class RE {
  constructor() {
    this.lastIndex = 0;
  }

  exec() {
    if (this.lastIndex === 0) {
      return [matchResult];
    }
    return null;
  }

  get global() {
    return true;
  }
}

var re = new RE();
var result = RegExp.prototype[Symbol.match].call(re, "");

assertNotNull(result);
assertSame(1, result.length);
assertSame("", result[0]);

assertSame(1, re.lastIndex);
