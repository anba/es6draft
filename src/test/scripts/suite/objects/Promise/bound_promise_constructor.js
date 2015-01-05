/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;


// 25.4.1.5.1 CreatePromiseCapabilityRecord, steps 4-5
// - [[Call]] on bound promise constructor will trigger TypeError because
//   thisArgument will be switched with [[BoundThis]]
{
  function BoundPromise() {
    const boundThis = null;
    return Object.setPrototypeOf(Promise.bind(boundThis), Promise);
  }

  assertThrows(TypeError, () => BoundPromise().all([]));
  assertThrows(TypeError, () => BoundPromise().race([]));
  assertThrows(TypeError, () => BoundPromise().reject(0));
  assertThrows(TypeError, () => BoundPromise().resolve(0));
  assertThrows(TypeError, () => { let p = new Promise(() => {}); p.constructor = BoundPromise(); p.then() });
}

// 25.4.1.5.1 CreatePromiseCapabilityRecord, steps 8
// - TypeError if constructorResult is different from promise argument
{
  function BoundPromise() {
    const boundThis = Promise[Symbol.create]();
    return Object.setPrototypeOf(Promise.bind(boundThis), Promise);
  }
  assertThrows(TypeError, () => BoundPromise().all([]));
  assertThrows(TypeError, () => BoundPromise().race([]));
  assertThrows(TypeError, () => BoundPromise().reject(0));
  assertThrows(TypeError, () => BoundPromise().resolve(0));
  assertThrows(TypeError, () => { let p = new Promise(() => {}); p.constructor = BoundPromise(); p.then() });
}
