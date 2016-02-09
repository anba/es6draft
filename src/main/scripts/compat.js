/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Compatibility() {
"use strict";

const global = %GlobalObject();

const {
  Object, Symbol, TypeError
} = global;

const {
  defineProperty: Object_defineProperty,
  getPrototypeOf: Object_getPrototypeOf,
  getOwnPropertyDescriptor: Object_getOwnPropertyDescriptor,
  prototype: {
    hasOwnProperty: Object_prototype_hasOwnProperty,
  }
} = Object;

/*
 * Add @@toStringTag to global object
 */
Object.defineProperty(global, Symbol.toStringTag, {
  value: "global", writable: true, enumerable: false, configurable: true
});

/*
 * Add __define[GS]etter__ and __lookup[GS]etter__
 */
Object.defineProperties(Object.assign(Object.prototype, {
  __defineGetter__(name, getter) {
    if (typeof getter != 'function') throw TypeError();
    var pk = %ToPropertyKey(name);
    var obj = this != null ? Object(this) : %GlobalThis();
    Object_defineProperty(obj, pk, {__proto__: null, get: getter, enumerable: true, configurable: true});
  },
  __defineSetter__(name, setter) {
    if (typeof setter != 'function') throw TypeError();
    var pk = %ToPropertyKey(name);
    var obj = this != null ? Object(this) : %GlobalThis();
    Object_defineProperty(obj, pk, {__proto__: null, set: setter, enumerable: true, configurable: true});
  },
  __lookupGetter__(name) {
    var pk = %ToPropertyKey(name);
    var p = this != null ? Object(this) : this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, pk);
      if (desc) {
        return %CallFunction(Object_prototype_hasOwnProperty, desc, "get") ? desc.get : void 0;
      }
    } while ((p = Object_getPrototypeOf(p)));
  },
  __lookupSetter__(name) {
    var pk = %ToPropertyKey(name);
    var p = this != null ? Object(this) : this;
    do {
      var desc = Object_getOwnPropertyDescriptor(p, pk);
      if (desc) {
        return %CallFunction(Object_prototype_hasOwnProperty, desc, "set") ? desc.set : void 0;
      }
    } while ((p = Object_getPrototypeOf(p)));
  }
}), {
  __defineGetter__: {enumerable: false},
  __defineSetter__: {enumerable: false},
  __lookupGetter__: {enumerable: false},
  __lookupSetter__: {enumerable: false},
});

if (!%IsCompatibilityOptionEnabled("StringTrim")) {
  const {String} = global;
  const trimLeftRE = /^\s+/, trimRightRE = /\s+$/;

  /*
   * Add 'trimLeft' and 'trimRight' to String.prototype
   */
  Object.defineProperties(Object.assign(String.prototype, {
    trimLeft() {
      if (this == null) throw TypeError();
      return %RegExpReplace(trimLeftRE, %ToString(this), "");
    },
    trimRight() {
      if (this == null) throw TypeError();
      return %RegExpReplace(trimRightRE, %ToString(this), "");
    },
  }), {
    trimLeft: {enumerable: false},
    trimRight: {enumerable: false},
  });
}

})();
