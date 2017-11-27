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

  // Ensure weak reference is strongly reachable.
  this.strongRef = weakRef;
}

const global = this;
let cleared = false;

function wait(n) {
  enqueueJob("finalizer", () => {
    if (!cleared) {
      cleared = true;
      global.strongRef.clear();
    }

    if (n > 0) {
      tryForceFinalization();
      wait(n - 1);
    }
  });
}

wait(5);
