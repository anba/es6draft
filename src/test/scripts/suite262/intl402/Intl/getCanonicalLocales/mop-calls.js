/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Ensure expected MOP traps are called.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)
    1. Let ll be ? CanonicalizeLocaleList(locales).
    2. Return CreateArrayFromList(ll).

  9.2.1 CanonicalizeLocaleList (locales)
    ...
    5. Let len be ? ToLength(? Get(O, "length")).
    6. Let k be 0.
    7. Repeat, while k < len
      a. Let Pk be ToString(k).
      b. Let kPresent be ? HasProperty(O, Pk).
      c. If kPresent is true, then
        i. Let kValue be ? Get(O, Pk).
        ...
features: [Proxy, Reflect]
includes: [proxyTrapsHelper.js, compareArray.js]
---*/

var locales = ["es-es", /* hole */, "se-se"];

var log = [];
var proxy = new Proxy(locales, allowProxyTraps({
  get: function(target, propertyKey, receiver) {
    log.push("get:" + String(propertyKey));
    return Reflect.get(target, propertyKey, receiver);
  },
  has: function(target, propertyKey) {
    log.push("has:" + String(propertyKey));
    return Reflect.has(target, propertyKey);
  },
}));

var canonicalLocales = Intl.getCanonicalLocales(proxy);

assert.sameValue(canonicalLocales.length, 2);
assert.sameValue(canonicalLocales[0], "es-ES");
assert.sameValue(canonicalLocales[1], "se-SE");

assert(compareArray(log, ["get:length", "has:0", "get:0", "has:1", "has:2", "get:2"]));
