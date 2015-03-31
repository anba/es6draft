/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertThrows, fail
} = Assert;

function evalFunDecl() {
  var t;
  var o = {
    g() {
      t = this;
    }
  };
  with (o) {
    eval(`
      function g() { return "ok"; }
      g();
    `);
  }
  assertSame(o, t);
  assertSame("ok", g());
}
evalFunDecl();

function evalVarDecl() {
  var t;
  var o = {
    g() {
      fail `not overridden`;
    }
  };
  assertThrows(ReferenceError, () => g);
  with (o) {
    eval(`
      var g = function() { t = this; }
      g();
    `);
  }
  assertSame(o, t);
  assertUndefined(g);
}
evalVarDecl();
