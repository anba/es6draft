/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Collection() {
"use strict";

const global = %GlobalObject();

const {
  Object, Function, Symbol
} = global;

// Create overrides for Map/Set/WeakMap/WeakSet to enable construction without `new`.

{ /* Map */
  const BuiltinMap = global.Map;
  const BuiltinMap_prototype_size = Object.getOwnPropertyDescriptor(BuiltinMap.prototype, "size").get;

  class Map extends BuiltinMap {
    constructor(iterable) {
      if (!%IsUninitializedMap(this)) {
        return new Map(iterable);
      }
      return %CallFunction(BuiltinMap, this, iterable);
    }

    clear() {
      return %CallFunction(BuiltinMap.prototype.clear, this);
    }

    delete(key) {
      return %CallFunction(BuiltinMap.prototype.delete, this, key);
    }

    entries() {
      return %CallFunction(BuiltinMap.prototype.entries, this);
    }

    forEach(callbackfn, thisArg = void 0) {
      return %CallFunction(BuiltinMap.prototype.forEach, this, callbackfn, thisArg);
    }

    get(key) {
      return %CallFunction(BuiltinMap.prototype.get, this, key);
    }

    has(key) {
      return %CallFunction(BuiltinMap.prototype.has, this, key);
    }

    keys() {
      return %CallFunction(BuiltinMap.prototype.keys, this);
    }

    set(key, value) {
      return %CallFunction(BuiltinMap.prototype.set, this, key, value);
    }

    get size() {
      return %CallFunction(BuiltinMap_prototype_size, this);
    }

    values() {
      return %CallFunction(BuiltinMap.prototype.values, this);
    }

    get [Symbol.toStringTag]() {
      return "Map";
    }
  }

  Object.setPrototypeOf(Map, Function.prototype);
  Object.setPrototypeOf(Map.prototype, Object.prototype);

  Object.defineProperties(Map.prototype, {
    clear: {enumerable: false},
    delete: {enumerable: false},
    entries: {enumerable: false},
    forEach: {enumerable: false},
    get: {enumerable: false},
    has: {enumerable: false},
    keys: {enumerable: false},
    set: {enumerable: false},
    size: {enumerable: false},
    values: {enumerable: false},
    [Symbol.iterator]: {value: Map.prototype.entries, writable: true, enumerable: false, configurable: true},
    [Symbol.toStringTag]: {enumerable: false},
  });

  %SetIntrinsic("Map", Map);
  global.Map = Map;
}

{ /* Set */
  const BuiltinSet = global.Set;
  const BuiltinSet_prototype_size = Object.getOwnPropertyDescriptor(BuiltinSet.prototype, "size").get;

  class Set extends BuiltinSet {
    constructor(iterable) {
      if (!%IsUninitializedSet(this)) {
        return new Set(iterable);
      }
      return %CallFunction(BuiltinSet, this, iterable);
    }

    add(value) {
      return %CallFunction(BuiltinSet.prototype.add, this, value);
    }

    clear() {
      return %CallFunction(BuiltinSet.prototype.clear, this);
    }

    delete(value) {
      return %CallFunction(BuiltinSet.prototype.delete, this, value);
    }

    entries() {
      return %CallFunction(BuiltinSet.prototype.entries, this);
    }

    forEach(callbackfn, thisArg = void 0) {
      return %CallFunction(BuiltinSet.prototype.forEach, this, callbackfn, thisArg);
    }

    has(value) {
      return %CallFunction(BuiltinSet.prototype.has, this, value);
    }

    get size() {
      return %CallFunction(BuiltinSet_prototype_size, this);
    }

    values() {
      return %CallFunction(BuiltinSet.prototype.values, this);
    }

    get [Symbol.toStringTag]() {
      return "Set";
    }
  }

  Object.setPrototypeOf(Set, Function.prototype);
  Object.setPrototypeOf(Set.prototype, Object.prototype);

  Object.defineProperties(Set.prototype, {
    add: {enumerable: false},
    clear: {enumerable: false},
    delete: {enumerable: false},
    entries: {enumerable: false},
    forEach: {enumerable: false},
    has: {enumerable: false},
    keys: {value: Set.prototype.values, writable: true, enumerable: false, configurable: true},
    size: {enumerable: false},
    values: {enumerable: false},
    [Symbol.iterator]: {value: Set.prototype.values, writable: true, enumerable: false, configurable: true},
    [Symbol.toStringTag]: {enumerable: false},
  });

  %SetIntrinsic("Set", Set);
  global.Set = Set;
}

{ /* WeakMap */
  const BuiltinWeakMap = global.WeakMap;

  class WeakMap extends BuiltinWeakMap {
    constructor(iterable) {
      if (!%IsUninitializedWeakMap(this)) {
        return new WeakMap(iterable);
      }
      return %CallFunction(BuiltinWeakMap, this, iterable);
    }

    clear() {
      // return %CallFunction(BuiltinWeakMap.prototype.clear, this);
      return %WeakMapClear(this);
    }

    delete(key) {
      return %CallFunction(BuiltinWeakMap.prototype.delete, this, key);
    }

    get(key, defaultValue) {
      if (%CallFunction(BuiltinWeakMap.prototype.has, this, key)) {
        return %CallFunction(BuiltinWeakMap.prototype.get, this, key);
      }
      return defaultValue;
    }

    has(key) {
      return %CallFunction(BuiltinWeakMap.prototype.has, this, key);
    }

    set(key, value) {
      return %CallFunction(BuiltinWeakMap.prototype.set, this, key, value);
    }

    get [Symbol.toStringTag]() {
      return "WeakMap";
    }
  }

  Object.setPrototypeOf(WeakMap, Function.prototype);
  Object.setPrototypeOf(WeakMap.prototype, Object.prototype);

  Object.defineProperties(WeakMap.prototype, {
    clear: {enumerable: false},
    delete: {enumerable: false},
    get: {enumerable: false},
    has: {enumerable: false},
    set: {enumerable: false},
    [Symbol.toStringTag]: {enumerable: false},
  });

  %SetIntrinsic("WeakMap", WeakMap);
  global.WeakMap = WeakMap;
}

{ /* WeakSet */
  const BuiltinWeakSet = global.WeakSet;

  class WeakSet extends BuiltinWeakSet {
    constructor(iterable) {
      return %CallFunction(BuiltinWeakSet, this, iterable);
    }

    add(value) {
      return %CallFunction(BuiltinWeakSet.prototype.add, this, value);
    }

    clear() {
      // return %CallFunction(BuiltinWeakSet.prototype.clear, this);
      return %WeakSetClear(this);
    }

    delete(value) {
      return %CallFunction(BuiltinWeakSet.prototype.delete, this, value);
    }

    has(value) {
      return %CallFunction(BuiltinWeakSet.prototype.has, this, value);
    }

    get [Symbol.toStringTag]() {
      return "WeakSet";
    }
  }

  Object.setPrototypeOf(WeakSet, Function.prototype);
  Object.setPrototypeOf(WeakSet.prototype, Object.prototype);

  Object.defineProperties(WeakSet.prototype, {
    add: {enumerable: false},
    clear: {enumerable: false},
    delete: {enumerable: false},
    has: {enumerable: false},
    [Symbol.toStringTag]: {enumerable: false},
  });

  %SetIntrinsic("WeakSet", WeakSet);
  global.WeakSet = WeakSet;
}

})();
