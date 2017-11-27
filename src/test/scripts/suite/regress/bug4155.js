/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.2.3.6 IteratorBindingInitialization, 13.2.3.7 KeyedBindingInitialization: Incorrect use for ResolveBinding
// https://bugs.ecmascript.org/show_bug.cgi?id=4155

var a = "global";

function f1(a = eval(`var a = 0; assertSame(0, a); "ok"`)) {
  return a;
}
assertSame("ok", f1());

function f2(a = (eval(`var a = 0;`), assertSame(0, a), "ok")) {
  return a;
}
assertSame("ok", f2());

function f3(a = (eval(`var a = 0;`), assertSame(0, a), () => a)) {
  assertSame(0, a());
  return "ok";
}
assertSame("ok", f3());

function f4({a = eval(`var a = 0; assertSame(0, a); "ok"`)} = {}) {
  return a;
}
assertSame("ok", f4());

function f5({a = (eval(`var a = 0;`), assertSame(0, a), "ok")} = {}) {
  return a;
}
assertSame("ok", f5());

function f6({a = (eval(`var a = 0;`), assertSame(0, a), () => a)} = {}) {
  assertSame(0, a());
  return "ok";
}
assertSame("ok", f6());
