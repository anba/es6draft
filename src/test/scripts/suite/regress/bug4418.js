/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 15.1.1 Early error WRT super in eval code too restrictive
// https://bugs.ecmascript.org/show_bug.cgi?id=4418

assertThrows(SyntaxError, () => (0, eval)("fail `unreachable`; super.p"));
assertThrows(SyntaxError, () => (0, eval)("fail `unreachable`; () => super.p"));
assertThrows(SyntaxError, () => (0, eval)("fail `unreachable`; () => () => super.p"));

let obj1 = {
  m() {
    eval("super.p");
    eval("() => super.p");
    eval("() => () => super.p");
  }
}
obj1.m();

let obj2 = {
  m() {
    eval("var f = () => eval('super.p'); f()");
  }
}
obj2.m();

assertThrows(SyntaxError, function() {
  eval("super.p");
});
