/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Compatibility(global) {
"use strict";

const Object = global.Object,
      String = global.String,
      TypeError = global.TypeError;

Object.defineProperty(global, getSym("@@toStringTag"), {
  value: "global", writable: true, enumerable: false, configurable: true
});

const Object_defineProperty = Object.defineProperty,
      Object_getPrototypeOf = Object.getPrototypeOf,
      Object_getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

function ToPropertyKey(pk) {
  // only string valued property keys here
  return String(pk);
}

Object.defineProperties(Object.assign(Object.prototype, {
  __defineGetter__(name, getter) {
    if (typeof getter != 'function') throw TypeError();
    var pk = ToPropertyKey(name);
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, pk, {get: getter, enumerable: true, configurable: true});
  },
  __defineSetter__(name, setter) {
    if (typeof setter != 'function') throw TypeError();
    var pk = ToPropertyKey(name);
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, pk, {set: setter, enumerable: true, configurable: true});
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

const String_prototype_replace = String.prototype.replace;

Object.defineProperties(Object.assign(String.prototype, {
  trimLeft() {
    return String_prototype_replace.call(this, /^\s+/, "");
  },
  trimRight() {
    return String_prototype_replace.call(this, /\s+$/, "");
  },
}), {
  trimLeft: {enumerable: false},
  trimRight: {enumerable: false},
});

})(this);
