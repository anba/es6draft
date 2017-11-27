/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

var x = "global";

function f1() {
  var x = "local";

  assertSame("local", x);
  class C {
    #private = do {
      var x = "do-expr";
    };
  }
  assertSame("local", x);
  new C();
  assertSame("local", x);
}
f1();

function f2() {
  assertSame("global", x);
  class C {
    #private = do {
      var x = "do-expr";
    };
  }
  assertSame("global", x);
  new C();
  assertSame("global", x);
}
f2();
