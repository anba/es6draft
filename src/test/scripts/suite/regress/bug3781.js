/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 13.6.4.13 ForIn/OfBodyEvaluation: Perform IteratorClose in 5.a - 5.g ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3781

class ThrowStepError extends Error { }
class ThrowDoneError extends Error { }
class ThrowValueError extends Error { }

class Iter {
  [Symbol.iterator]() {
    return this;
  }

  return() {
    fail `return() called`;
  }
}

class ThrowStep extends Iter {
  next() {
    throw new ThrowStepError();
  }
}

class ThrowDone extends Iter {
  next() {
    return {
      value: 0,
      get done() { throw new ThrowDoneError() },
    };
  }
}

class ThrowValue extends Iter {
  next() {
    return {
      get value() { throw new ThrowValueError() },
      done: false,
    };
  }
}

for (let C of [Map, Set, WeakMap, WeakSet]) {
  assertThrows(ThrowStepError, () => new C(new ThrowStep));
  assertThrows(ThrowDoneError, () => new C(new ThrowDone));
  assertThrows(ThrowValueError, () => new C(new ThrowValue));
}

class NotPromise {
  constructor(executor) {
    executor(() => fail `success callback called`, e => { throw e });
  }
}

for (let m of [Promise.all, Promise.race]) {
  assertThrows(ThrowStepError, () => m.call(NotPromise, new ThrowStep));
  assertThrows(ThrowDoneError, () => m.call(NotPromise, new ThrowDone));
  assertThrows(ThrowValueError, () => m.call(NotPromise, new ThrowValue));
}

assertThrows(ThrowStepError, () => { let [x] = new ThrowStep });
assertThrows(ThrowDoneError, () => { let [x] = new ThrowDone });
assertThrows(ThrowValueError, () => { let [x] = new ThrowValue });

assertThrows(ThrowStepError, () => { const [x] = new ThrowStep });
assertThrows(ThrowDoneError, () => { const [x] = new ThrowDone });
assertThrows(ThrowValueError, () => { const [x] = new ThrowValue });

assertThrows(ThrowStepError, () => { var [x] = new ThrowStep });
assertThrows(ThrowDoneError, () => { var [x] = new ThrowDone });
assertThrows(ThrowValueError, () => { var [x] = new ThrowValue });

assertThrows(ThrowStepError, () => { var x; [x] = new ThrowStep });
assertThrows(ThrowDoneError, () => { var x; [x] = new ThrowDone });
assertThrows(ThrowValueError, () => { var x; [x] = new ThrowValue });
