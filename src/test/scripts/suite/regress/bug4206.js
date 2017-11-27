/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse
} = Assert;

// 25.2.4: 'name' property omitted from GeneratorInstance
// https://bugs.ecmascript.org/show_bug.cgi?id=4206

function* decl() {}
var expr1 = function* named(){};
var expr2 = function* (){};
var expr3; (expr3) = function* (){};

assertSame("decl", decl.name);
assertSame("named", expr1.name);
assertSame("expr2", expr2.name);
assertSame("", expr3.name);
assertFalse(expr3.hasOwnProperty("name"));
