/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 21.2.5.7 RegExp.prototype.replace: Missing string index checks in step 18
// https://bugs.ecmascript.org/show_bug.cgi?id=2849

let result = (new class extends RegExp {
  exec(){
    return {index: 0, 0: "abc"};
  }
})[Symbol.replace]("a", "b");

assertSame("b", result);
