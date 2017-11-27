/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Direct Eval, Strict Mode, VariableEnvironment=GlobalEnv, LexicalEnvironment=GlobalEnv

"use strict";

const {
  assertTrue,
  assertFalse,
  assertSame,
  assertUndefined,
} = Assert;

const global = this;

eval("function v1(){}");
assertSame("undefined", typeof v1);
assertFalse("v1" in global);
assertUndefined(global["v1"]);

eval("function* v2(){}");
assertSame("undefined", typeof v2);
assertFalse("v2" in global);
assertUndefined(global["v2"]);

eval("class v3{}");
assertSame("undefined", typeof v3);
assertFalse("v3" in global);
assertUndefined(global["v3"]);

eval("var v4");
assertSame("undefined", typeof v4);
assertFalse("v4" in global);
assertUndefined(global["v4"]);

eval("var v5 = 0");
assertSame("undefined", typeof v5);
assertFalse("v5" in global);
assertUndefined(global["v5"]);

eval("let v6");
assertSame("undefined", typeof v6);
assertFalse("v6" in global);
assertUndefined(global["v6"]);

eval("let v7 = 0");
assertSame("undefined", typeof v7);
assertFalse("v7" in global);
assertUndefined(global["v7"]);

eval("const v8 = 0");
assertSame("undefined", typeof v8);
assertFalse("v8" in global);
assertUndefined(global["v8"]);
