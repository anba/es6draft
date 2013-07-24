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
      WeakSet = global.WeakSet;

const Array_prototype_join = Array.prototype.join,
      Array_prototype_toLocaleString = Array.prototype.toLocaleString,
      Error_prototype_toString = Error.prototype.toString,
      Set_prototype_add = Set.prototype.add,
      Set_prototype_delete = Set.prototype.delete,
      Set_prototype_has = Set.prototype.has,
      WeakSet_prototype_add = WeakSet.prototype.add,
      WeakSet_prototype_delete = WeakSet.prototype.delete,
      WeakSet_prototype_has = WeakSet.prototype.has;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);
const weakset = new WeakSet(), set = new Set();

function checkCycle(fn, o, ...args) {
  if (typeof o == 'function' || typeof o == 'object' && o !== null) {
    if ($CallFunction(WeakSet_prototype_has, weakset, o)) {
      return "";
    }
    $CallFunction(WeakSet_prototype_add, weakset, o);
  } else {
    if ($CallFunction(Set_prototype_has, set, o)) {
      return "" + o;
    }
    $CallFunction(Set_prototype_add, set, o);
  }
  try {
    return $CallFunction(fn, o, ...args);
  } finally {
    if (typeof o == 'function' || typeof o == 'object' && o !== null) {
      $CallFunction(WeakSet_prototype_delete, weakset, o);
    } else {
      $CallFunction(Set_prototype_delete, set, o);
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
