/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Unclear semantics of call site object caching (12.2.8.2.2)
// https://bugs.ecmascript.org/show_bug.cgi?id=3305

let sites = [];
function g(o) { sites.push(o) }
function f() { eval("g`bla`"); }
f();
f();
eval("g`bla`");
Function("g`bla`")();
Function("g`bla`")();

assertSame(5, sites.length);
assertSame(sites[0], sites[1]);
assertSame(sites[0], sites[2]);
assertSame(sites[0], sites[3]);
assertSame(sites[0], sites[4]);
