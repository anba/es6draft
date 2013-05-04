/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function ItObject(global) {
"use strict";

const Object = global.Object,
      Error = global.Error,
      Proxy = global.Proxy,
      Reflect = global.Reflect;

var it_custom = undefined;

const it_mapped = {
  __proto__: null,
  custom: true,
  customRdOnly: false,
};

const it_target = Object.create(Object.prototype, {
  color: {writable: true, enumerable: true, configurable: true},
  height: {writable: true, enumerable: true, configurable: true},
  width: {writable: true, enumerable: true, configurable: true},
  funny: {writable: true, enumerable: true, configurable: true},
  array: {writable: true, enumerable: true, configurable: true},
  rdonly: {writable: false, enumerable: true, configurable: true},
  custom: {writable: true, enumerable: true, configurable: true},
  customRdOnly: {writable: false, enumerable: true, configurable: true},
  customNative: {
    get() { if (this === it) return it_custom },
    set(v) { if (this === it) it_custom = v },
    enumerable: true, configurable: true
  }
});

Object.defineProperty(it_target, getSym("@@toStringTag"), {
  value: "It"
});

const it = Proxy(it_target, {
  getOwnPropertyDescriptor(t, pk) {
    if (pk in it_mapped) {
      return Object.assign(Reflect.getOwnPropertyDescriptor(t, pk), {value: it_custom});
    }
    return Reflect.getOwnPropertyDescriptor(t, pk);
  },
  defineProperty(t, pk, desc) {
    delete it_mapped[pk];
    return Reflect.defineProperty(t, pk, desc);
  },
  get(t, pk, r) {
    if (pk in it_mapped) {
      return it_custom;
    }
    return Reflect.get(t, pk, r);
  },
  set(t, pk, v, r) {
    if (pk in it_mapped) {
      if (it_mapped[pk]) {
        it_custom = v;
      }
      return it_mapped[pk];
    }
    return Reflect.set(t, pk, v, r);
  },
  deleteProperty(t, pk) {
    delete it_mapped[pk];
    return Reflect.deleteProperty(t, pk);
  },
  enumerate(t) {
    if (it.enum_fail) {
      throw Error("its enumeration failed");
    }
    return Reflect.enumerate(t);
  },
});

Object.defineProperty(global, "it", {
  value: it,
  writable: true, enumerable: false, configurable: true
});

})(this);
