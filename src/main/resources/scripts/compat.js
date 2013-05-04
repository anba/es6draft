/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Compatibility(global) {
"use strict";

const Object = global.Object,
      String = global.String;

Object.defineProperty(global, getSym("@@toStringTag"), {
  value: "global", writable: true, enumerable: false, configurable: true
});

const Object_defineProperty = Object.defineProperty,
      Object_getOwnPropertyDescriptor = Object.getOwnPropertyDescriptor;

Object.defineProperties(Object.assign(Object.prototype, {
  __defineGetter__(name, getter) {
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, name, {get: getter, enumerable: true, configurable: true});
  },
  __defineSetter__(name, setter) {
    var obj = (this != null ? Object(this) : global);
    Object_defineProperty(obj, name, {set: setter, enumerable: true, configurable: true});
  },
  __lookupGetter__(name) {
    var p = this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, name);
      if (desc && desc.get) return desc.get;
    } while ((p = p.__proto__));
  },
  __lookupSetter__(name) {
    var p = this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, name);
      if (desc && desc.set) return desc.set;
    } while ((p = p.__proto__));
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
