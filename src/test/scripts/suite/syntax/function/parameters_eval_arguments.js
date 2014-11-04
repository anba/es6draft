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

// Non-strict, parameter list with initializer
{
  function f1(a = 0, eval) { }
  function f2(a = 0, arguments) { }
}

// Non-strict, parameter list with rest parameter
{
  function f1(...eval) { }
  function f2(...arguments) { }
}

// Non-strict, parameter list with object binding pattern
{
  function f1({eval}) { }
  function f2({arguments}) { }
}

// Non-strict, parameter list with array binding pattern
{
  function f1([eval]) { }
  function f2([arguments]) { }
}

// Strict, simple parameter list
{
  assertSyntaxError(`"use strict"; function f1(eval) { }`);
  assertSyntaxError(`"use strict"; function f2(arguments) { }`);
}

// Strict, parameter list with initializer
{
  assertSyntaxError(`"use strict"; function f1(a = 0, eval) { }`);
  assertSyntaxError(`"use strict"; function f2(a = 0, arguments) { }`);
}
