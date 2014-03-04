/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// Indexed access on uninitialised typed array throws TypeError from currently active realm

const foreignRealm = new Realm();
foreignRealm.eval(`
  function indexedAccess(ta) {
    ta[0];
  }
`);

let int8Array = Int8Array[Symbol.create]();
assertThrows(() => int8Array[0], TypeError);
assertThrows(() => foreignRealm.global.indexedAccess(int8Array), foreignRealm.global.TypeError);
