/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Non-strict mode, 'yield' and GeneratorComprehension in FunctionDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  function f1() { (for (yield of []) 0) }
  function f2() { (for ({yield} of []) 0) }
  function f3() { (for ({yield = 0} of []) 0) }
  function f4() { (for ({x: yield} of []) 0) }
  function f5() { (for ({x: yield = 0} of []) 0) }
  function f6() { (for ({x: y = yield} of []) 0) }
  function f7() { (for ({x: y = yield * 2} of []) 0) }
  function f8() { (for ([yield] of []) 0) }
  function f9() { (for ([x = yield] of []) 0) }
  function f10() { (for ([x = yield * 2] of []) 0) }
  function f11() { (for ([...yield] of []) 0) }
  function f12() { (for (a of [yield]) 0) }
  function f13() { (for (a of [yield * 2]) 0) }
  function f14() { (for (a of []) yield) }
  function f15() { (for (a of []) yield * 2) }
}

// Strict mode, 'yield' and GeneratorComprehension in FunctionDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`"use strict"; function f1() { (for (yield of []) 0) }`);
  assertSyntaxError(`"use strict"; function f2() { (for ({yield} of []) 0) }`);
  assertSyntaxError(`"use strict"; function f3() { (for ({yield = 0} of []) 0) }`);
  assertSyntaxError(`"use strict"; function f4() { (for ({x: yield} of []) 0) }`);
  assertSyntaxError(`"use strict"; function f5() { (for ({x: yield = 0} of []) 0) }`);
  assertSyntaxError(`"use strict"; function f6() { (for ({x: y = yield} of []) 0) }`);
  assertSyntaxError(`"use strict"; function f7() { (for ({x: y = yield * 2} of []) 0) }`);
  assertSyntaxError(`"use strict"; function f8() { (for ([yield] of []) 0) }`);
  assertSyntaxError(`"use strict"; function f9() { (for ([x = yield] of []) 0) }`);
  assertSyntaxError(`"use strict"; function f10() { (for ([x = yield * 2] of []) 0) }`);
  assertSyntaxError(`"use strict"; function f11() { (for ([...yield] of []) 0) }`);
  assertSyntaxError(`"use strict"; function f12() { (for (a of [yield]) 0) }`);
  assertSyntaxError(`"use strict"; function f13() { (for (a of [yield * 2]) 0) }`);
  assertSyntaxError(`"use strict"; function f14() { (for (a of []) yield) }`);
  assertSyntaxError(`"use strict"; function f15() { (for (a of []) yield * 2) }`);
}

// Non-strict mode, 'yield' and GeneratorComprehension in GeneratorDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`function* f1() { (for (yield of []) 0) }`);
  assertSyntaxError(`function* f2() { (for ({yield} of []) 0) }`);
  assertSyntaxError(`function* f3() { (for ({yield = 0} of []) 0) }`);
  assertSyntaxError(`function* f4() { (for ({x: yield} of []) 0) }`);
  assertSyntaxError(`function* f5() { (for ({x: yield = 0} of []) 0) }`);
  assertSyntaxError(`function* f6() { (for ({x: y = yield} of []) 0) }`);
  assertSyntaxError(`function* f7() { (for ({x: y = yield * 2} of []) 0) }`);
  assertSyntaxError(`function* f8() { (for ([yield] of []) 0) }`);
  assertSyntaxError(`function* f9() { (for ([x = yield] of []) 0) }`);
  assertSyntaxError(`function* f10() { (for ([x = yield * 2] of []) 0) }`);
  assertSyntaxError(`function* f11() { (for ([...yield] of []) 0) }`);
  assertSyntaxError(`function* f12() { (for (a of [yield]) 0) }`);
  assertSyntaxError(`function* f13() { (for (a of [yield * 2]) 0) }`);
  assertSyntaxError(`function* f14() { (for (a of []) yield) }`);
  assertSyntaxError(`function* f15() { (for (a of []) yield * 2) }`);
}

// Strict mode, 'yield' and GeneratorComprehension in GeneratorDeclaration
{
  // 'yield' as IdentifierReference, BindingIdentifier
  assertSyntaxError(`function* f1() {"use strict"; (for (yield of []) 0) }`);
  assertSyntaxError(`function* f2() {"use strict"; (for ({yield} of []) 0) }`);
  assertSyntaxError(`function* f3() {"use strict"; (for ({yield = 0} of []) 0) }`);
  assertSyntaxError(`function* f4() {"use strict"; (for ({x: yield} of []) 0) }`);
  assertSyntaxError(`function* f5() {"use strict"; (for ({x: yield = 0} of []) 0) }`);
  assertSyntaxError(`function* f6() {"use strict"; (for ({x: y = yield} of []) 0) }`);
  assertSyntaxError(`function* f7() {"use strict"; (for ({x: y = yield * 2} of []) 0) }`);
  assertSyntaxError(`function* f8() {"use strict"; (for ([yield] of []) 0) }`);
  assertSyntaxError(`function* f9() {"use strict"; (for ([x = yield] of []) 0) }`);
  assertSyntaxError(`function* f10() {"use strict"; (for ([x = yield * 2] of []) 0) }`);
  assertSyntaxError(`function* f11() {"use strict"; (for ([...yield] of []) 0) }`);
  assertSyntaxError(`function* f12() {"use strict"; (for (a of [yield]) 0) }`);
  assertSyntaxError(`function* f13() {"use strict"; (for (a of [yield * 2]) 0) }`);
  assertSyntaxError(`function* f14() {"use strict"; (for (a of []) yield) }`);
  assertSyntaxError(`function* f15() {"use strict"; (for (a of []) yield * 2) }`);
}
