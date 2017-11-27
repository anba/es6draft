/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  fail
} = Assert;

function tryForceFinalization() {
  gc(); gc(); runFinalization();
}

let exec = () => fail `finalize executor called`;

{
  let target = {};
  let weakRef = System.makeWeakRef(target, exec);
  target = null;

  // Weak reference is no longer reachable after exiting this block.
}

function wait(n) {
  enqueueJob("finalizer", () => {
    if (n > 0) {
      tryForceFinalization();
      wait(n - 1);
    }
  });
}

wait(5);
