/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertUndefined, assertThrows
} = Assert;

// WeakMap/Set.get/has should throw TypeError
// https://bugs.ecmascript.org/show_bug.cgi?id=4019

for (let C of [WeakMap, WeakSet]) {
  var weakCollection = new C();
  for (let prim of [void 0, null, 0, 1, NaN, false, true, "", Symbol()]) {
    assertFalse(weakCollection.has(prim));
    assertFalse(weakCollection.delete(prim));
    if (C === WeakMap) {
      assertUndefined(weakCollection.get(prim));
      assertThrows(TypeError, () => weakCollection.set(prim, null));
    } else {
      assertThrows(TypeError, () => weakCollection.add(prim));
    }
  }
}
