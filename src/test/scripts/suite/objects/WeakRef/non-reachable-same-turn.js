/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotUndefined
} = Assert;

function tryForceFinalization() {
  gc(); gc(); runFinalization();
}

{
  let target = {};
  let weakRef = System.makeWeakRef(target);
  target = null;

  tryForceFinalization();

  assertNotUndefined(weakRef.get());
}
