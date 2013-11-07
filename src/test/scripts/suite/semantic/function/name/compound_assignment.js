/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse
} = Assert;

function assertAnonymousFunction(f) {
  return assertFalse(f.hasOwnProperty("name"));
}

// anonymous function/generator expression, compound assignment does not define .name property
{
  let f0, g0, a0, c0;

  f0 += function() {};
  assertAnonymousFunction(f0);

  g0 += function*() {};
  assertAnonymousFunction(g0);

  a0 += () => {};
  assertAnonymousFunction(a0);

  c0 += class {};
  assertAnonymousFunction(c0);
}
