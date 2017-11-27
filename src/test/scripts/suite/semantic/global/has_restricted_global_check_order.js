/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals,
} = Assert;

let loggingEnabled = false;
let log = [];
let newRealm = new Reflect.Realm({}, new Proxy({
  getOwnPropertyDescriptor(t, pk, desc) {
    log.push(`getOwn:${String(pk)}`);
    return Reflect.getOwnPropertyDescriptor(t, pk, desc);
  }
}, {
  get(t, pk, r){
    if (loggingEnabled) {
      log.push(`trap:${pk}`);
    }
    return Reflect.get(t, pk, r);
  }
}));
loggingEnabled = true;

evalScript("let z, a;", {realm: newRealm});

assertEquals([
  "trap:getOwnPropertyDescriptor",
  "getOwn:z",
  "trap:getOwnPropertyDescriptor",
  "getOwn:a",
], log);
