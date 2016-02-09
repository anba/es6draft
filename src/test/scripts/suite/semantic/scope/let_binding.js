/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertTrue
} = Assert;

// Iteration bindings are copied before update expression
{
  let arr = [];
  for (let i = 0; i < 3; ++i) {
    arr[i] = () => i;
  }
  for (let i = 0; i < 3; ++i) {
    assertSame(i, arr[i]());
  }
}

// Iteration bindings are copied before first entry and before update expression
{
  let values = [1, 1, 2, 3, 4];
  for (let i = 0, inc = () => i++, j = inc(); i < 5; inc = () => i++) {
    assertSame(values.shift(), i);
    inc();
  }
  assertSame(0, values.length);
}

// Iteration bindings are copied before update expression (combined test)
{
  let body = [], test = [], update = [];
  for (let i = 0; test.push(() => i), i < 3; update.push(() => i), ++i) {
    body.push(() => i);
  }
  assertSame(3, body.length);
  for (let i = 0; i < 3; ++i) {
    assertSame(i, body[i]());
  }
  assertSame(4, test.length);
  for (let i = 0; i < 4; ++i) {
    assertSame(i, test[i]());
  }
  assertSame(3, update.length);
  for (let i = 0; i < 3; ++i) {
    assertSame(i + 1, update[i]());
  }
}

// Zeroth and first iteration have different bindings
{
  let enteredLoop = false;
  let zeroth, first;
  for (let i = 0, z = (zeroth = () => i); i < 1;) {
    assertFalse(enteredLoop);
    enteredLoop = true;
    first = () => i;
    i += 1;
    assertSame(1, i);
  }
  assertTrue(enteredLoop);
  assertSame(0, zeroth());
  assertSame(1, first());
}
