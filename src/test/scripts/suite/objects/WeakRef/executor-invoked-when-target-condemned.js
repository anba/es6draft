/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined, assertSame
} = Assert;

function tryForceFinalization() {
  gc(); gc(); runFinalization();
}

let execCalled = false;

{
  let target = {};
  let holdings = {};
  let weakRef = System.makeWeakRef(target, h => {
    execCalled = true;
    assertUndefined(weakRef.get());
    assertSame(holdings, h);
  }, holdings);
  target = null;

  // Ensure weak reference is strongly reachable.
  this.strongRef = weakRef;
}

function wait() {
  enqueueJob("finalizer", () => {
    if (!execCalled) {
      tryForceFinalization();
      wait();
    }
  });
}

wait();
