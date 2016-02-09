/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

loadRelativeToScript("./resources/concurrent.js");

{
  // 0 -> Barrier:Mutex
  // 1 -> Barrier:Event
  // 2 -> Barrier:Needed
  // 3 -> Barrier:InitialNeeded
  // 4 -> Counter
  // 5 -> Futex
  setSharedArrayBuffer(new SharedArrayBuffer(4 * 6));

  let workers = 4;
  let ta = new Int32Array(getSharedArrayBuffer());
  ta.set([0, 0, workers, workers, workers, 0]);

  for (let i = 0; i < workers; ++i) {
    let started = evalInWorker(`
      loadRelativeToScript("./resources/concurrent.js");

      let ta = new Int32Array(getSharedArrayBuffer());
      let b = new Barrier(ta, 0, 1, 2, 3);

      b.wait();

      if (Atomics.sub(ta, 4, 1) === 1) {
        while (Atomics.futexWake(ta, 5, 1) !== 1) ;
      }
    `);
    assertTrue(started);
  }

  let r = Atomics.futexWait(ta, 5, 0);
  assertSame(Atomics.OK, r);
  assertSame(Atomics.load(ta, 4), 0);

  setSharedArrayBuffer(null);
}
