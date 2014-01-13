/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const count = 19;

function r(s) { var n = count; while(n-->0) s += s; return s }

// ArrayLiteral
eval("function f() {return; [" + r("g(),") + "] }")

// ObjectLiteral
eval("function f() {return; ({" + r("a:g(),") + "}) }")

// CommaExpression
eval("function f() {return; (" + r("g(),") + "0) }")

// top-level statements
eval("function f() {return; " + r("g();") + " }")

// top-level statements in ArrowFunction
eval("function f() {() => {return; " + r("g();") + "} }")

// nested statements
// - broken at count=19
// eval("function f() {return; {" + r("g();",19) + "} }")

// nested statements in ArrowFunction
// - broken at count=19
// eval("function f() {()=>{return; {" + r("g();",19) + "}} }")
