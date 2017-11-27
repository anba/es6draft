/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.1 super and new.target should not be allowed in direct evals
// https://bugs.ecmascript.org/show_bug.cgi?id=4041

var obj = {
  __proto__: {
    p: "ok",
  },
  get q() {
    return eval("super.p");
  },
  set r(x) {
    return eval("super.p");
  },
  m() {
    return eval("super.p");
  },
  *g() {
    return eval("super.p");
  },
};

assertSame("ok", obj.q);
assertSame("ok", Object.getOwnPropertyDescriptor(obj, "r").set());
assertSame("ok", obj.m());
assertSame("ok", obj.g().next().value);

function C() {
  return eval("new.target");
}
function D() { }
assertSame(C, new C);
assertSame(C, Reflect.construct(C, []));
assertSame(C, Reflect.construct(C, [], C));
assertSame(D, Reflect.construct(C, [], D));
