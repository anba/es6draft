/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Non-strict mode, 'yield' in default parameter expression in FunctionDeclaration
{
  function f1(a = yield) { }
  function f2(a = yield(0)) { }
}

// Strict mode, 'yield' in default parameter expression in FunctionDeclaration
{
  assertSyntaxError(`function f1(a = yield) { "use strict" }`);
  assertSyntaxError(`function f2(a = yield(0)) { "use strict" }`);
}

// Non-strict mode, 'yield' in default parameter expression in GeneratorDeclaration
{
  assertSyntaxError(`function* f1(a = yield) { }`);
  assertSyntaxError(`function* f2(a = yield(0)) { }`);
}

// Strict mode, 'yield' in default parameter expression in GeneratorDeclaration
{
  assertSyntaxError(`function* f1(a = yield) { "use strict" }`);
  assertSyntaxError(`function* f2(a = yield(0)) { "use strict" }`);
}
