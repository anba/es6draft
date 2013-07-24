/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Compatibility(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      String = global.String,
      RegExp = global.RegExp,
      TypeError = global.TypeError;

const Object_defineProperty = Object.defineProperty,
      Object_getPrototypeOf = Object.getPrototypeOf,
      Object_getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

function ToPropertyKey(pk) {
  if (typeof pk == 'symbol') {
    return pk;
  }
  return String(pk);
}

Object.defineProperty(global, global.getSym("@@toStringTag"), {
  value: "global", writable: true, enumerable: false, configurable: true
});

Object.defineProperties(Object.assign(Object.prototype, {
  __defineGetter__(name, getter) {
    if (typeof getter != 'function') throw new TypeError();
    var pk = ToPropertyKey(name);
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, pk, {__proto__: null, get: getter, enumerable: true, configurable: true});
  },
  __defineSetter__(name, setter) {
    if (typeof setter != 'function') throw new TypeError();
    var pk = ToPropertyKey(name);
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, pk, {__proto__: null, set: setter, enumerable: true, configurable: true});
  },
  __lookupGetter__(name) {
    var pk = ToPropertyKey(name);
    var p = (this != null ? Object(this) : this);
    do {
      var desc = Object_getOwnPropertyDescriptor(p, pk);
      if (desc) return desc.get;
    } while ((p = Object_getPrototypeOf(p)));
  },
  __lookupSetter__(name) {
    var pk = ToPropertyKey(name);
    var p = (this != null ? Object(this) : this);
    do {
      var desc = Object_getOwnPropertyDescriptor(p, pk);
      if (desc) return desc.set;
    } while ((p = Object_getPrototypeOf(p)));
  }
}), {
  __defineGetter__: {enumerable: false},
  __defineSetter__: {enumerable: false},
  __lookupGetter__: {enumerable: false},
  __lookupSetter__: {enumerable: false},
});

const RegExp_prototype_replace = RegExp.prototype.replace;
const trimLeftRE = /^\s+/, trimRightRE = /\s+$/;

Object.defineProperties(Object.assign(String.prototype, {
  trimLeft() {
    if (this == null) throw new TypeError();
    return $CallFunction(RegExp_prototype_replace, trimLeftRE, "" + this, "");
  },
  trimRight() {
    if (this == null) throw new TypeError();
    return $CallFunction(RegExp_prototype_replace, trimRightRE, "" + this, "");
  },
}), {
  trimLeft: {enumerable: false},
  trimRight: {enumerable: false},
});

})(this);
