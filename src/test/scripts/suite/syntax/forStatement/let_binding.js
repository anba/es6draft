/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertFalse, assertTrue
} = Assert;

// Iteration bindings are copied after update expression
{
  let arr = [];
  for (let i = 0; i < 3; ++i) {
    arr[i] = () => i;
  }
  for (let i = 0; i < 3; ++i) {
    assertSame(i + 1, arr[i]());
  }
}

// Iteration bindings are copied before test expression
{
  let countDown = 10;
  for (let i = 0, inc = () => i++, j = inc(); i < 5; inc = () => i++) {
    assertSame(1, i);
    inc();
    if (--countDown === 0) {
      break;
    }
  }
  assertSame(0, countDown);
}

// Zeroth and first iteration have different bindings
{
  let enteredLoop = false;
  let zeroth, first;
  for (let i = 0, z = (zeroth = () => i); i < 1; ++i) {
    assertFalse(enteredLoop);
    enteredLoop = true;
    first = () => i;
    assertSame(0, i);
  }
  assertTrue(enteredLoop);
  assertSame(0, zeroth());
  assertSame(1, first());
}
