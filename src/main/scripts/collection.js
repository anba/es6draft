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
  Object, Function, Symbol, TypeError,
} = global;

const Object_defineProperty = Object.defineProperty,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty);

const {
  create: createSym,
} = Symbol;

// Pseudo-symbol in SpiderMonkey
const mozIteratorSym = "@@iterator";

// Create overrides for Map/Set/WeakMap/WeakSet:
// - To enable construction without `new`
// - To enable initialisation with `mozIteratorSym`
// - Map, Set and WeakSet can be patched by sub-classing, WeakMap requires delegation to pass all tests

function isObjectWithBrand(o, sym) {
  if (typeof o !== 'object' || o === null) {
    return false;
  }
  if (!Object_hasOwnProperty(o, sym) || o[sym] !== false) {
    return false;
  }
  Object_defineProperty(o, sym, {__proto__: null, value: true, configurable: false});
  return true;
}

function addBrand(o, sym) {
  return Object_defineProperty(o, sym, {__proto__: null, value: false, configurable: true});
}

{ /* Map */
  const BuiltinMap = global.Map;
  const isMapSym = Symbol("isMap");

  class Map extends BuiltinMap {
    constructor(iterable) {
      if (!isObjectWithBrand(this, isMapSym)) {
        return new Map(iterable);
      }
      if (iterable !== void 0) {
        iterable = iterable[mozIteratorSym]();
      }
      return super(iterable);
    }

    // overridden to change return value to `undefined`
    set(key, value) {
      super(key, value);
    }

    // overriden to pass surface tests
    get size() {
      return super.size;
    }

    static [createSym]() {
      return addBrand(super(), isMapSym);
    }
  }

  Object.defineProperties(Map.prototype, {
    set: {enumerable: false},
    size: {enumerable: false},
  });

  Object.defineProperties(Map, {
    [createSym]: {writable: false, enumerable: false},
  });

  %SetIntrinsic("Map", Map);
  global.Map = Map;
}

{ /* Set */
  const BuiltinSet = global.Set;
  const isSetSym = Symbol("isSet");

  class Set extends BuiltinSet {
    constructor(iterable) {
      if (!isObjectWithBrand(this, isSetSym)) {
        return new Set(iterable);
      }
      if (iterable !== void 0) {
        iterable = iterable[mozIteratorSym]();
      }
      return super(iterable);
    }

    // overridden to change return value to `undefined`
    add(value) {
      super(value);
    }

    // overriden to pass surface tests
    get size() {
      return super.size;
    }

    static [createSym]() {
      return addBrand(super(), isSetSym);
    }
  }

  Object.defineProperties(Set.prototype, {
    add: {enumerable: false},
    size: {enumerable: false},
  });

  Object.defineProperties(Set, {
    [createSym]: {writable: false, enumerable: false},
  });

  %SetIntrinsic("Set", Set);
  global.Set = Set;
}

{ /* WeakMap */
  const BuiltinWeakMap = global.WeakMap;
  const isWeakMapSym = Symbol("isWeakMap");

  class WeakMap {
    constructor(iterable) {
      if (!isObjectWithBrand(this, isWeakMapSym)) {
        return new WeakMap(iterable);
      }
      if (iterable !== void 0) {
        iterable = iterable[mozIteratorSym]();
      }
      return BuiltinWeakMap.call(this, iterable);
    }

    clear() {
      return BuiltinWeakMap.prototype.clear.call(this);
    }

    delete(key) {
      return BuiltinWeakMap.prototype.delete.call(this, key);
    }

    get(key, defaultValue) {
      if (BuiltinWeakMap.prototype.has.call(this, key)) {
        return BuiltinWeakMap.prototype.get.call(this, key);
      }
      return defaultValue;
    }

    has(key) {
      return BuiltinWeakMap.prototype.has.call(this, key);
    }

    set(key, value) {
      // No `return` here because tests require `undefined` as return value.
      BuiltinWeakMap.prototype.set.call(this, key, value);
    }

    get [Symbol.toStringTag]() {
      return "WeakMap";
    }

    static [createSym]() {
      let wm = BuiltinWeakMap[createSym].call(this);
      return addBrand(wm, isWeakMapSym);
    }
  }

  Object.defineProperties(WeakMap.prototype, {
    clear: {enumerable: false},
    delete: {enumerable: false},
    get: {enumerable: false},
    has: {enumerable: false},
    set: {enumerable: false},
    [Symbol.toStringTag]: {enumerable: false},
  });

  Object.defineProperties(WeakMap, {
    [createSym]: {writable: false, enumerable: false},
  });

  %SetIntrinsic("WeakMap", WeakMap);
  global.WeakMap = WeakMap;
}

{ /* WeakSet */
  const BuiltinWeakSet = global.WeakSet;
  const isWeakSetSym = Symbol("isWeakSet");

  class WeakSet extends BuiltinWeakSet {
    constructor(iterable) {
      if (!isObjectWithBrand(this, isWeakSetSym)) {
        return new WeakSet(iterable);
      }
      if (iterable !== void 0) {
        iterable = iterable[mozIteratorSym]();
      }
      return super(iterable);
    }

    // overridden to change return value to `undefined`
    add(value) {
      super(value);
    }

    static [createSym]() {
      return addBrand(super(), isWeakSetSym);
    }
  }

  Object.defineProperties(WeakSet.prototype, {
    add: {enumerable: false},
  });

  Object.defineProperties(WeakSet, {
    [createSym]: {writable: false, enumerable: false},
  });

  %SetIntrinsic("WeakSet", WeakSet);
  global.WeakSet = WeakSet;
}

})();
