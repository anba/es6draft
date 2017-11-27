/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
this.Realm = {
  __id: 0,
  __map: new Map([[0, getCurrentRealm()]]),
  __shared: void 0,
  current() {
    let thisRealm = getCurrentRealm();
    for (let [realmId, realm] of this.__map) {
      if (thisRealm === realm) {
        return realmId;
      }
    }
    return 0;
  },
  create() {
    let realmId = ++this.__id;
    let realm = new Reflect.Realm();
    realm.global.Realm = this;
    this.__map.set(realmId, realm);
    return realmId;
  },
  dispose(realmId) {
    this.__map.delete(realmId);
  },
  eval(realmId, source) {
    let realm = this.__map.get(realmId);
    return evalInRealm(realm, source);
  },
  global(realmId) {
    let realm = this.__map.get(realmId);
    return realm.global;
  },
  get shared() {
    return this.__shared;
  },
  set shared(value) {
    this.__shared = value;
  },
};
