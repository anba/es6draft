/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function ItObject(global) {
"use strict";

const {
  Object, Function, Error, Proxy, Reflect,
} = global;

const Object_assign = Object.assign;

const {
  getOwnPropertyDescriptor: Reflect_getOwnPropertyDescriptor,
  defineProperty: Reflect_defineProperty,
  get: Reflect_get,
  set: Reflect_set,
  deleteProperty: Reflect_deleteProperty,
  enumerate: Reflect_enumerate,
} = Reflect;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

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
  },
  [global.Symbol.toStringTag]: {value: "It"},
});

const it = new Proxy(it_target, {
  getOwnPropertyDescriptor(t, pk) {
    if (pk in it_mapped) {
      return Object_assign(Reflect_getOwnPropertyDescriptor(t, pk), {value: it_custom});
    }
    return Reflect_getOwnPropertyDescriptor(t, pk);
  },
  defineProperty(t, pk, desc) {
    delete it_mapped[pk];
    return Reflect_defineProperty(t, pk, desc);
  },
  get(t, pk, r) {
    if (pk in it_mapped) {
      return it_custom;
    }
    return Reflect_get(t, pk, r);
  },
  set(t, pk, v, r) {
    if (pk in it_mapped) {
      if (it_mapped[pk]) {
        it_custom = v;
      }
      return it_mapped[pk];
    }
    return Reflect_set(t, pk, v, r);
  },
  invoke(t, pk, args, r) {
    var f = this.get(t, pk, r);
    return $CallFunction(f, r, ...args);
  },
  deleteProperty(t, pk) {
    delete it_mapped[pk];
    return Reflect_deleteProperty(t, pk);
  },
  enumerate(t) {
    if (it.enum_fail) {
      throw new Error("its enumeration failed");
    }
    return Reflect_enumerate(t);
  },
});

Object.defineProperty(global, "it", {
  value: it,
  writable: true, enumerable: false, configurable: true
});

})(this);
