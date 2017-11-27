/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, assertTrue, assertFalse
} = Assert;

// 22.2.2.1.2 TypedArrayAllocOrInit: Assert not entirely correct in step 3
// https://bugs.ecmascript.org/show_bug.cgi?id=2906

// let lengthCalled = false;
// let ta = new class extends Int8Array { constructor() { /* no super */ } };

// assertThrows(TypeError, () => {
//   Int8Array.call(ta, {
//     get length() {
//       assertFalse(lengthCalled);
//       lengthCalled = true;
//       Object.getPrototypeOf(Int8Array).call(ta, 1);
//       return 0;
//     }
//   });
// });

// assertTrue(lengthCalled);
// assertSame(1, ta.length);
