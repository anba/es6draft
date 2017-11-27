/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Cyclic() {
"use strict";

const Set = %Intrinsic("Set");
const {
  add: Set_prototype_add,
  delete: Set_prototype_delete,
  has: Set_prototype_has,
} = Set.prototype;

const set = new Set();

function checkCycle(fn, o, ...args) {
  if (%CallFunction(Set_prototype_has, set, o)) {
    if (typeof o == 'function' || typeof o == 'object' && o !== null) {
      return "";
    }
    return "" + o;
  }
  %CallFunction(Set_prototype_add, set, o);
  try {
    return %CallFunction(fn, o, ...args);
  } finally {
    %CallFunction(Set_prototype_delete, set, o);
  }
}

// Add cyclic check to: Array.prototype.join and Array.prototype.toLocaleString
const ArrayPrototype = %Intrinsic("ArrayPrototype");
const {
  join: Array_prototype_join,
  toLocaleString: Array_prototype_toLocaleString,
} = ArrayPrototype;

%CreateMethodProperties(ArrayPrototype, {
  join(separator) {
    return checkCycle(Array_prototype_join, this, separator);
  },
  toLocaleString() {
    return checkCycle(Array_prototype_toLocaleString, this);
  }
});

// Add cyclic check to: Error.prototype.toString
const ErrorPrototype = %Intrinsic("ErrorPrototype");
const Error_prototype_toString = ErrorPrototype.toString;

%CreateMethodProperties(ErrorPrototype, {
  toString() {
    return checkCycle(Error_prototype_toString, this);
  }
});

})();
