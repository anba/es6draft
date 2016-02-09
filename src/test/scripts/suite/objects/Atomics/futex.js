/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertEquals
} = Assert;

// Steps:
// - Create worker and wait in worker
// - Wake worker and wait until worker writes to shared memory
{
  // 0 -> Futex
  // 1 -> Data
  setSharedArrayBuffer(new SharedArrayBuffer(64));

  const ta = new Int32Array(getSharedArrayBuffer());
  Atomics.store(ta, 1, 123);

  let worker = evalInWorker(`
    const ta = new Int32Array(getSharedArrayBuffer());
    let result = Atomics.futexWait(ta, 0, 0);
    Atomics.store(ta, 1, result);
    while (Atomics.futexWake(ta, 1, 1) === 0) ;
  `);
  assertTrue(worker);

  assertSame(123, Atomics.load(ta, 1));

  let woken;
  do { woken = Atomics.futexWake(ta, 0, 1); } while (woken === 0);
  assertSame(1, woken);

  let result = Atomics.futexWait(ta, 1, 123);

  assertTrue(result === Atomics.OK || result === Atomics.NOTEQUAL);
  assertSame(Atomics.OK, Atomics.load(ta, 1));

  setSharedArrayBuffer(null);
}

{
  // 0 -> Counter (worker)
  // 1 -> Futex (worker->main)
  // 2 -> Futex (main->worker)
  // 3 -> Futex (main->worker)
  // 4 -> Data  (worker)
  // 5 -> Futex (main)
  setSharedArrayBuffer(new SharedArrayBuffer(4 * 6));

  const ta = new Int32Array(getSharedArrayBuffer());

  for (let i = 0; i < 3; ++i) {
    let worker = evalInWorker(`
      const ta = new Int32Array(getSharedArrayBuffer());
      if (Atomics.add(ta, 0, 1) < 2) {
        Atomics.futexWait(ta, 2, 0);
      }
      Atomics.store(ta, 4, ${i});
      while (Atomics.futexWake(ta, 1, 1) === 0) ;
    `);
    assertTrue(worker);
  }

  // Wait until workers have started.
  do {
    Atomics.futexWait(ta, 5, 0, 50);
  } while (Atomics.load(ta, 0) !== 3);

  Atomics.futexWait(ta, 1, 0);
  let first = Atomics.load(ta, 4);

  while (Atomics.futexWakeOrRequeue(ta, 2, 1, 3, 0) === 0) ;

  Atomics.futexWait(ta, 1, 0);
  let second = Atomics.load(ta, 4);

  while (Atomics.futexWake(ta, 3, 1) === 0) ;

  Atomics.futexWait(ta, 1, 0);
  let third = Atomics.load(ta, 4);

  assertEquals([0, 1, 2], [first, second, third].sort());

  setSharedArrayBuffer(null);
}
