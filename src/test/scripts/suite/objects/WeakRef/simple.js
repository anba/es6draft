/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

{
  let target = {};
  let weakRef = System.makeWeakRef(target);

  assertSame(target, weakRef.get());
}

{
  let target = {};
  let weakRef = System.makeWeakRef(target);

  weakRef.clear();

  assertUndefined(weakRef.get());
}
