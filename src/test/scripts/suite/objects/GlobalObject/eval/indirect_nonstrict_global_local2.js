/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Indirect Eval, Not Strict Mode, VariableEnvironment=GlobalEnv, LexicalEnvironment=not GlobalEnv

const {
  assertTrue,
  assertFalse,
  assertSame,
  assertUndefined,
  assertNotUndefined,
} = Assert;

const global = this;
let obj;

with (obj = {}) {
(1,eval)("function v1(){}");
assertSame("function", typeof v1);
assertTrue("v1" in global);
assertNotUndefined(global["v1"]);
assertFalse("v1" in obj);
assertUndefined(obj["v1"]);
}
with (obj = {}) {
(1,eval)("function* v2(){}");
assertSame("function", typeof v2);
assertTrue("v2" in global);
assertNotUndefined(global["v2"]);
assertFalse("v2" in obj);
assertUndefined(obj["v2"]);
}
with (obj = {}) {
(1,eval)("class v3{}");
assertSame("function", typeof v3);
assertFalse("v3" in global);
assertUndefined(global["v3"]);
assertFalse("v3" in obj);
assertUndefined(obj["v3"]);
}
with (obj = {}) {
(1,eval)("var v4");
assertSame("undefined", typeof v4);
assertTrue("v4" in global);
assertUndefined(global["v4"]);
assertFalse("v4" in obj);
assertUndefined(obj["v4"]);
}
with (obj = {}) {
(1,eval)("var v5 = 0");
assertSame("number", typeof v5);
assertTrue("v5" in global);
assertNotUndefined(global["v5"]);
assertFalse("v5" in obj);
assertUndefined(obj["v5"]);
}
with (obj = {}) {
(1,eval)("let v6");
assertSame("undefined", typeof v6);
assertFalse("v6" in global);
assertUndefined(global["v6"]);
assertFalse("v6" in obj);
assertUndefined(obj["v6"]);
}
with (obj = {}) {
(1,eval)("let v7 = 0");
assertSame("number", typeof v7);
assertFalse("v7" in global);
assertUndefined(global["v7"]);
assertFalse("v7" in obj);
assertUndefined(obj["v7"]);
}
with (obj = {}) {
(1,eval)("const v8 = 0");
assertSame("number", typeof v8);
assertFalse("v8" in global);
assertUndefined(global["v8"]);
assertFalse("v8" in obj);
assertUndefined(obj["v8"]);
}
