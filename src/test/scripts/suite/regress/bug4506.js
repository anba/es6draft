/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertInstanceOf, assertTrue
} = Assert;

// 18.2.1.2 EvalDeclarationInstantiation: Use TypeError instead of SyntaxError in steps 8 and 10
// https://bugs.ecmascript.org/show_bug.cgi?id=4506

var error = undefined;
try {
  eval("function NaN(){}");
} catch (e) {
  error = e;
}
assertInstanceOf(TypeError, error);

assertTrue(Reflect.preventExtensions(this));

var error = undefined;
try {
  eval("var nonExistentVariableName");
} catch (e) {
  error = e;
}
assertInstanceOf(TypeError, error);
