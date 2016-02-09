/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// See U. Drepper, "Futexes are tricky".
{
  const mutexTypedArray = Symbol(),
        valueIndex = Symbol();
  class Mutex {
    constructor(typedArray, value) {
      this[mutexTypedArray] = typedArray;
      this[valueIndex] = value;
    }

    lock() {
      const {
        [mutexTypedArray]: typedArray,
        [valueIndex]: value,
      } = this;
      let c;
      if ((c = Atomics.compareExchange(typedArray, value, 0, 1)) !== 0) {
        if (c !== 2) {
          c = Atomics.exchange(typedArray, value, 2);
        }
        while (c !== 0) {
          Atomics.futexWait(typedArray, value, 2);
          c = Atomics.exchange(typedArray, value, 2);
        }
      }
    }

    unlock() {
      const {
        [mutexTypedArray]: typedArray,
        [valueIndex]: value,
      } = this;
      if (Atomics.sub(typedArray, value, 1) !== 1) {
        Atomics.store(typedArray, value, 0);
        Atomics.futexWake(typedArray, value, 1);
      }
    }
  }

  const barrierTypedArray = Symbol(),
        lock = Symbol(),
        eventIndex = Symbol(),
        stillNeededIndex = Symbol(),
        initialNeededIndex = Symbol();
  class Barrier {
    constructor(typedArray, mutex, event, needed, initial) {
      this[barrierTypedArray] = typedArray;
      this[lock] = new Mutex(typedArray, mutex);
      this[eventIndex] = event;
      this[stillNeededIndex] = needed;
      this[initialNeededIndex] = initial;
    }

    wait() {
      const {
        [barrierTypedArray]: typedArray,
        [lock]: mutex,
        [eventIndex]: event,
        [stillNeededIndex]: stillNeeded,
        [initialNeededIndex]: initialNeeded,
      } = this;
      mutex.lock();
      if (Atomics.sub(typedArray, stillNeeded, 1) > 1) {
        const ev = Atomics.load(typedArray, event);
        mutex.unlock();
        do {
          Atomics.futexWait(typedArray, event, ev);
        } while (Atomics.load(typedArray, event) === ev);
      } else {
        Atomics.add(typedArray, event, 1);
        Atomics.store(typedArray, stillNeeded, Atomics.load(typedArray, initialNeeded));
        Atomics.futexWake(typedArray, event, Infinity);
        mutex.unlock();
      }
    }
  }

  this.Mutex = Mutex;
  this.Barrier = Barrier;
}
