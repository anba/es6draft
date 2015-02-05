/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Collection() {
"use strict";

const global = %GlobalObject();

const {
  Object, WeakMap, WeakSet
} = global;

const {
  has: WeakMap_prototype_has,
  get: WeakMap_prototype_get,
} = WeakMap.prototype;

// Add modified WeakMap.prototype.get
Object.defineProperty(Object.assign(WeakMap.prototype, {
  get(key, defaultValue) {
    if (%CallFunction(WeakMap_prototype_has, this, key)) {
      return %CallFunction(WeakMap_prototype_get, this, key);
    }
    return defaultValue;
  }
}), "get", {enumerable: false});

// Add WeakMap.prototype.clear
Object.defineProperty(Object.assign(WeakMap.prototype, {
  clear() {
    return %WeakMapClear(this);
  }
}), "clear", {enumerable: false});

// Add WeakSet.prototype.clear
Object.defineProperty(Object.assign(WeakSet.prototype, {
  clear() {
    return %WeakSetClear(this);
  }
}), "clear", {enumerable: false});

})();
