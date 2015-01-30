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
// - [[Call]] on bound promise constructor does not throw TypeError
{
  function BoundPromise() {
    const boundThis = null;
    return Object.setPrototypeOf(Promise.bind(boundThis), Promise);
  }

  BoundPromise().all([]);
  BoundPromise().race([]);
  BoundPromise().reject(0);
  BoundPromise().resolve(0);
  { let p = new Promise(() => {}); p.constructor = BoundPromise(); p.then() }
}
