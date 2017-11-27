/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Indexed access on detached typed array throws TypeError from currently active realm

const foreignRealm = new Reflect.Realm();
foreignRealm.eval(`
  function indexedAccess(ta) {
    ta[0];
  }
`);

let int8Array = new Int8Array(0);
detachArrayBuffer(int8Array.buffer);

assertThrows(TypeError, () => int8Array[0]);
assertThrows(foreignRealm.global.TypeError, () => foreignRealm.global.indexedAccess(int8Array));
