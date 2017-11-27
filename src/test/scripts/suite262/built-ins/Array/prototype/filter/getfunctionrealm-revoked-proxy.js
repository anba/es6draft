/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-getfunctionrealm
info: |
  GetFunctionRealm throws TypeError for revoked proxies.
description: >
  22.1.3.1 Array.prototype.concat ( ...arguments )

  1. Let O be ? ToObject(this value).
  ...
  5. Let A be ? ArraySpeciesCreate(O, 0).
  ...

  9.4.2.3 ArraySpeciesCreate ( originalArray, length )

  ...
  6. If IsConstructor(C) is true, then
    a. Let thisRealm be the current Realm Record.
    b. Let realmC be ? GetFunctionRealm(C).
    ...

  7.3.22 GetFunctionRealm ( obj )

  ...
  4. If obj is a Proxy exotic object, then
    a. If obj.[[ProxyHandler]] is null, throw a TypeError exception.
  ...

includes: [proxyTrapsHelper.js]
---*/

var rp = Proxy.revocable(function() {}, {});
var p = new Proxy(rp.proxy, allowProxyTraps(null));

var a = [];
a.constructor = p;

rp.revoke();

assert.throws(TypeError, function() {
  a.filter(function() {});
});
