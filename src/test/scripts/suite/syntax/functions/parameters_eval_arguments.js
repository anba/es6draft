/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 'eval' and 'arguments' as parameters names

// Non-strict, simple parameter list
{
  function f1(eval) { }
  function f2(arguments) { }
}

// Non-strict, not simple parameter list
{
  function f1(a = 0, eval) { }
  function f2(a = 0, arguments) { }
}

// Strict, simple parameter list
{
  assertSyntaxError(`"use strict"; function f1(eval) { }`);
  assertSyntaxError(`"use strict"; function f2(arguments) { }`);
}

// Strict, not simple parameter list
{
  assertSyntaxError(`"use strict"; function f1(a = 0, eval) { }`);
  assertSyntaxError(`"use strict"; function f2(a = 0, arguments) { }`);
}
