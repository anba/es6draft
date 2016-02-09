/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, fail
} = Assert;

// 7.3.21 GetFunctionRealm: Handle revoked proxies
// https://bugs.ecmascript.org/show_bug.cgi?id=3018

{
  let {proxy, revoke} = Proxy.revocable(
    function() { fail `trap not called` },
    {
      apply() { fail `apply called` },
      construct() { fail `construct called` },
    }
  );
  let foreignRealm = new Reflect.Realm();
  let foreignArray = foreignRealm.global.Array;
  function callArrayMap() {
    let array = [];
    array.constructor = proxy;
    return foreignArray.prototype.map.call(array, () => {});
  }

  // Proxy as constructor is from wrong realm, Array.prototype.map creates result array from own realm
  assertSame(foreignArray.prototype, Reflect.getPrototypeOf(callArrayMap()));

  revoke();

  // Revoked proxy realm defaults to current realm, that leads to calling proxy[[Construct]] on the revoked proxy
  assertThrows(foreignRealm.global.TypeError, callArrayMap);
}
