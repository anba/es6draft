/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// Non-strict mode, 'yield' and ArrowFunction in FunctionDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  function f1() { () => yield; }
  function f2() { yield => 0; }
  function f3() { (yield) => 0; }
  function f4() { (yield = yield * 2) => 0; }
  function f5() { (a = yield * 2) => 0; }
  function f6() { (...yield) => 0; }
  function f7() { (a, ...yield) => 0; }
}

// Strict mode, 'yield' and ArrowFunction in FunctionDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`"use strict"; function f1() { () => yield; }`);
  assertSyntaxError(`"use strict"; function f2() { yield => 0; }`);
  assertSyntaxError(`"use strict"; function f3() { (yield) => 0; }`);
  assertSyntaxError(`"use strict"; function f4() { (yield = yield * 2) => 0; }`);
  assertSyntaxError(`"use strict"; function f5() { (a = yield * 2) => 0; }`);
  assertSyntaxError(`"use strict"; function f6() { (...yield) => 0; }`);
  assertSyntaxError(`"use strict"; function f7() { (a, ...yield) => 0; }`);
}

// Non-strict mode, 'yield' and ArrowFunction in GeneratorDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  function* f1() { () => yield; }
  assertSyntaxError(`function* f2() { yield => 0; }`);
  function* f3() { (yield) => 0; }
  assertSyntaxError(`function* f4() { (yield = yield * 2) => 0; }`);
  function* f5() { (a = yield * 2) => 0; }
  assertSyntaxError(`function* f6() { (...yield) => 0; }`);
  assertSyntaxError(`function* f7() { (a, ...yield) => 0; }`);
}

// Strict mode, 'yield' and ArrowFunction in GeneratorDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`"use strict"; function* f1() { () => yield; }`);
  assertSyntaxError(`"use strict"; function* f2() { yield => 0; }`);
  assertSyntaxError(`"use strict"; function* f3() { (yield) => 0; }`);
  assertSyntaxError(`"use strict"; function* f4() { (yield = yield * 2) => 0; }`);
  assertSyntaxError(`"use strict"; function* f5() { (a = yield * 2) => 0; }`);
  assertSyntaxError(`"use strict"; function* f6() { (...yield) => 0; }`);
  assertSyntaxError(`"use strict"; function* f7() { (a, ...yield) => 0; }`);
}
