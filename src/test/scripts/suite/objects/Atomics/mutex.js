/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

loadRelativeToScript("./resources/concurrent.js");

{
  // 0 -> Mutex
  // 1 -> Data
  // 2 -> Futex
  setSharedArrayBuffer(new SharedArrayBuffer(4 * 3));

  let ta = new Int32Array(getSharedArrayBuffer());

  evalInWorker(`
    loadRelativeToScript("./resources/concurrent.js");

    let ta = new Int32Array(getSharedArrayBuffer());

    while (Atomics.futexWait(ta, 2, 0) !== Atomics.OK) ;

    let m = new Mutex(ta, 0);
    m.lock();
    try {
      Atomics.store(ta, 1, Atomics.load(ta, 1) * 3);
    } finally {
      m.unlock();
    }

    while (Atomics.futexWake(ta, 2, 1) !== 1) ;
  `);

  let m = new Mutex(ta, 0);
  m.lock();
  try {
    while (Atomics.futexWake(ta, 2, 1) !== 1) ;

    // spin
    for (let i = 0; i < 1000; ++i) ;

    Atomics.store(ta, 1, 123);
  } finally {
    m.unlock();
  }

  while (Atomics.futexWait(ta, 2, 0) !== Atomics.OK) ;

  assertSame(3 * 123, Atomics.load(ta, 1));

  setSharedArrayBuffer(null);
}
