/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// Similar to "yield_arrow.js", except arrow function is in default parameter initializer of generator

// Non-strict mode, 'yield' and ArrowFunction in FunctionDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  function f1(p = () => yield) { }
  function f2(p = yield => 0) { }
  function f3(p = (yield) => 0) { }
  function f4(p = (yield = yield * 2) => 0) { }
  function f5(p = (a = yield * 2) => 0) { }
  function f6(p = (...yield) => 0) { }
  function f7(p = (a, ...yield) => 0) { }
  function f8(p = ([yield]) => 0) { }
  function f9(p = ([...yield]) => 0) { }
  function f10(p = ({yield}) => 0) { }
  function f11(p = ({a: yield}) => 0) { }
  function f12(p = ({yield: yield}) => 0) { }
}

// Strict mode, 'yield' and ArrowFunction in FunctionDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`"use strict"; function f1(p = () => yield) { }`);
  assertSyntaxError(`"use strict"; function f2(p = yield => 0) { }`);
  assertSyntaxError(`"use strict"; function f3(p = (yield) => 0) { }`);
  assertSyntaxError(`"use strict"; function f4(p = (yield = yield * 2) => 0) { }`);
  assertSyntaxError(`"use strict"; function f5(p = (a = yield * 2) => 0) { }`);
  assertSyntaxError(`"use strict"; function f6(p = (...yield) => 0) { }`);
  assertSyntaxError(`"use strict"; function f7(p = (a, ...yield) => 0) { }`);
  assertSyntaxError(`"use strict"; function f8(p = ([yield]) => 0) { }`);
  assertSyntaxError(`"use strict"; function f9(p = ([...yield]) => 0) { }`);
  assertSyntaxError(`"use strict"; function f10(p = ({yield}) => 0) { }`);
  assertSyntaxError(`"use strict"; function f11(p = ({a: yield}) => 0) { }`);
  assertSyntaxError(`"use strict"; function f12(p = ({yield: yield}) => 0) { }`);
}

// Non-strict mode, 'yield' and ArrowFunction in GeneratorDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  function* f1(p = () => yield) { }
  function* f2(p = yield => 0) { }
  function* f3(p = (yield) => 0) { }
  function* f4(p = (yield = yield * 2) => 0) { }
  function* f5(p = (a = yield * 2) => 0) { }
  function* f6(p = (...yield) => 0) { }
  function* f7(p = (a, ...yield) => 0) { }
  function* f8(p = ([yield]) => 0) { }
  function* f9(p = ([...yield]) => 0) { }
  function* f10(p = ({yield}) => 0) { }
  function* f11(p = ({a: yield}) => 0) { }
  function* f12(p = ({yield: yield}) => 0) { }
}

// Strict mode, 'yield' and ArrowFunction in GeneratorDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`"use strict"; function* f1(p = () => yield) { }`);
  assertSyntaxError(`"use strict"; function* f2(p = yield => 0) { }`);
  assertSyntaxError(`"use strict"; function* f3(p = (yield) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f4(p = (yield = yield * 2) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f5(p = (a = yield * 2) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f6(p = (...yield) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f7(p = (a, ...yield) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f8(p = ([yield]) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f9(p = ([...yield]) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f10(p = ({yield}) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f11(p = ({a: yield}) => 0) { }`);
  assertSyntaxError(`"use strict"; function* f12(p = ({yield: yield}) => 0) { }`);
}
