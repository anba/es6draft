/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

for (let TArray of [Int32Array, Int16Array, Int8Array, Uint32Array, Uint16Array, Uint8Array]) {
  // 0 -> Futex
  // 1 [0] -> CompareExchange Data
  // 2 [1] -> Result Worker

  setSharedArrayBuffer(new SharedArrayBuffer(4 * 3));
  let fta = new Int32Array(getSharedArrayBuffer(), 0, 1);
  let ta = new TArray(getSharedArrayBuffer(), 4, 2);
  const iterations = 1000;

  evalInWorker(`
    let fta = new Int32Array(getSharedArrayBuffer(), 0, 1);
    let ta = new ${TArray.name}(getSharedArrayBuffer(), 4, 2);

    for (let i = 0; i < ${iterations}; ++i) {
      // Wait until state is reset.
      while (Atomics.wait(fta, 0, 0) !== "ok") ;

      Atomics.store(ta, 1, Atomics.compareExchange(ta, 0, 3, 1));

      // Notify worker has finished.
      while (Atomics.wake(fta, 0, 1) !== 1) ;
    }
  `);

  for (let i = 0; i < iterations; ++i) {
    // Reset state.
    Atomics.store(ta, 0, 3);

    // Wake worker.
    while (Atomics.wake(fta, 0, 1) !== 1) ;

    let r = Atomics.compareExchange(ta, 0, 1, 2);

    // Wait until worker finished.
    while (Atomics.wait(fta, 0, 0) !== "ok") ;

    assertSame(3, Atomics.load(ta, 1));
    if (r === 1) {
      assertSame(2, Atomics.load(ta, 0));
    } else {
      assertSame(3, r);
      assertSame(1, Atomics.load(ta, 0));
    }
  }
}
