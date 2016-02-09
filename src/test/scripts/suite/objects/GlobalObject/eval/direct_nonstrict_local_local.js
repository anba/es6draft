/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// Direct Eval, Not Strict Mode, VariableEnvironment=not GlobalEnv, LexicalEnvironment=not GlobalEnv

const {
  assertTrue,
  assertFalse,
  assertSame,
  assertUndefined,
} = Assert;

const global = this;

(function(){
  eval("function v1(){}");
  assertSame("function", typeof v1);
  assertFalse("v1" in global);
  assertUndefined(global["v1"]);
})();
(function(){
  eval("function* v2(){}");
  assertSame("function", typeof v2);
  assertFalse("v2" in global);
  assertUndefined(global["v2"]);
})();
(function(){
  eval("class v3{}");
  assertSame("undefined", typeof v3);
  assertFalse("v3" in global);
  assertUndefined(global["v3"]);
})();
(function(){
  eval("var v4");
  assertSame("undefined", typeof v4);
  assertFalse("v4" in global);
  assertUndefined(global["v4"]);
})();
(function(){
  eval("var v5 = 0");
  assertSame("number", typeof v5);
  assertFalse("v5" in global);
  assertUndefined(global["v5"]);
})();
(function(){
  eval("let v6");
  assertSame("undefined", typeof v6);
  assertFalse("v6" in global);
  assertUndefined(global["v6"]);
})();
(function(){
  eval("let v7 = 0");
  assertSame("undefined", typeof v7);
  assertFalse("v7" in global);
  assertUndefined(global["v7"]);
})();
(function(){
  eval("const v8 = 0");
  assertSame("undefined", typeof v8);
  assertFalse("v8" in global);
  assertUndefined(global["v8"]);
})();
