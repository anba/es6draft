/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertUndefined
} = Assert;

{
  let a = [];
  a[0] = 'A';
  a[100000] = 'B';

  assertSame(100000 + 1, a.length);
  assertSame('A', a[0]);
  assertSame('B', a[100000]);

  a.length = 1;

  assertSame(1, a.length);
  assertSame('A', a[0]);
  assertUndefined(a[100000]);
}

{
  let a = [];
  a[0] = 'A';
  a[100000 - 2] = 'B2';
  a[100000 - 1] = 'B1';
  a[100000] = 'B';

  assertSame(100000 + 1, a.length);
  assertSame('A', a[0]);
  assertSame('B2', a[100000 - 2]);
  assertSame('B1', a[100000 - 1]);
  assertSame('B', a[100000]);

  a.length = 1;

  assertSame(1, a.length);
  assertSame('A', a[0]);
  assertUndefined(a[100000 - 2]);
  assertUndefined(a[100000 - 1]);
  assertUndefined(a[100000]);
}

{
  let a = [];
  a[0] = 'A';
  a[100000] = 'B';
  Object.defineProperty(a, 100000 - 1, {configurable: false, value: 'B1'});

  assertSame(100000 + 1, a.length);
  assertSame('A', a[0]);
  assertSame('B1', a[100000 - 1]);
  assertSame('B', a[100000]);

  a.length = 1;

  assertSame(100000, a.length);
  assertSame('A', a[0]);
  assertSame('B1', a[100000 - 1]);
  assertUndefined(a[100000]);
}
