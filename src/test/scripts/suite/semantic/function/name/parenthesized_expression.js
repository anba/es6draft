/*
 * Copyright (c) André Bargull
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

// anonymous function/generator expression
{
  let f0, g0, a0, c0;

  f0 = (1, function() {});
  assertAnonymousFunction(f0);

  g0 = (1, function*() {});
  assertAnonymousFunction(g0);

  a0 = (1, () => {});
  assertAnonymousFunction(a0);

  c0 = (1, class {});
  assertAnonymousFunction(c0);
}
