/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.2.2.1.1 TypedArrayFrom: Add steps to initialize typed array when @@iterator not present
// https://bugs.ecmascript.org/show_bug.cgi?id=2784

{
  let ta = new class extends Int8Array { constructor() { /* no super */ } };
  let result = Int8Array.call(ta, Object.assign({length: 0}, []));
  assertSame(ta, result);
  assertSame(0, ta.length);
}

{
  let ta = new class extends Int8Array { constructor() { /* no super */ } };
  let result = Int8Array.call(ta, Object.assign({length: 3}, [1, 2, 3]));
  assertSame(ta, result);
  assertSame(3, ta.length);
  assertSame(1, ta[0]);
  assertSame(2, ta[1]);
  assertSame(3, ta[2]);
}
