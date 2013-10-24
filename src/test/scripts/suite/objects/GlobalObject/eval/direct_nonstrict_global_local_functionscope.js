/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Direct Eval, Not Strict Mode, VariableEnvironment=GlobalEnv, LexicalEnvironment=not GlobalEnv
// - Testing [[Scope]] for functions

const {
  assertTrue,
  assertSame,
  assertNotUndefined,
} = Assert;

const global = this;

var w = "global-w";
let x = "global-x";
var y = "global-y";
let z = "global-z";
let obj = {w: "local-w", x: "local-x"};


// WithStatement with inner BlockStatement
with (obj) {
  eval("function f1() { return [w, x, y, z] }");
}

// f1 installed in object-environment of global-environment
assertSame("function", typeof f1);
assertTrue("f1" in global);
assertNotUndefined(global["f1"]);
// [[Scope]] for f1 includes `obj` lexical environment
assertSame("local-w", f1()[0]);
assertSame("local-x", f1()[1]);
assertSame("global-y", f1()[2]);
assertSame("global-z", f1()[3]);


// WithStatement without inner BlockStatement
with (obj) eval("function f2() { return [w, x, y, z] }");

// f2 installed in object-environment of global-environment
assertSame("function", typeof f2);
assertTrue("f2" in global);
assertNotUndefined(global["f2"]);
// [[Scope]] for f2 includes `obj` lexical environment
assertSame("local-w", f2()[0]);
assertSame("local-x", f2()[1]);
assertSame("global-y", f2()[2]);
assertSame("global-z", f2()[3]);


// Catch block
try { throw obj } catch ({w, x}) {
  eval("function f3() { return [w, x, y, z] }");
}

// f3 installed in object-environment of global-environment
assertSame("function", typeof f3);
assertTrue("f3" in global);
assertNotUndefined(global["f3"]);
// [[Scope]] for f3 includes `obj` lexical environment
assertSame("local-w", f3()[0]);
assertSame("local-x", f3()[1]);
assertSame("global-y", f3()[2]);
assertSame("global-z", f3()[3]);


// BlockStatement
{
  var w = "local-w";
  let x = "local-x";
  eval("function f4() { return [w, x, y, z] }");
}

// f4 installed in object-environment of global-environment
assertSame("function", typeof f4);
assertTrue("f4" in global);
assertNotUndefined(global["f4"]);
// [[Scope]] for f4 includes `obj` lexical environment
assertSame("local-w", f4()[0]);
assertSame("local-x", f4()[1]);
assertSame("global-y", f4()[2]);
assertSame("global-z", f4()[3]);

// `var w` clobbers global-w, restore it here
assertSame("local-w", w);
w = "global-w";

