/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Cyclic(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      Array = global.Array,
      Error = global.Error,
      Set = global.Set,
      WeakMap = global.WeakMap;

const Array_prototype_join = Array.prototype.join,
      Array_prototype_toLocaleString = Array.prototype.toLocaleString,
      Error_prototype_toString = Error.prototype.toString;

const Function_call = Function.prototype.call.bind(Function.prototype.call);
const wm = new WeakMap(), cs = new Set();

function checkCycle(fn, o, ...args) {
  if (typeof this == 'function' || typeof o == 'object' && o !== null) {
    if (wm.has(o)) {
      return "";
    }
    wm.set(o, null);
  } else {
    if (cs.has(o)) {
      return "" + o;
    }
    cs.add(o);
  }
  try {
    return Function_call(fn, o, ...args);
  } finally {
    if (typeof this == 'function' || typeof o == 'object' && o !== null) {
      wm.delete(o);
    } else {
      cs.delete(o);
    }
  }
}

Object.defineProperty(Object.assign(Array.prototype, {
  join(separator) {
    return checkCycle(Array_prototype_join, this, separator);
  }
}), "join", {enumerable: false});

Object.defineProperty(Object.assign(Array.prototype, {
  toLocaleString() {
    return checkCycle(Array_prototype_toLocaleString, this);
  }
}), "toLocaleString", {enumerable: false});

Object.defineProperty(Object.assign(Error.prototype, {
  toString() {
    return checkCycle(Error_prototype_toString, this);
  }
}), "toString", {enumerable: false});

})(this);
