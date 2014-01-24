/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Collection(global) {
"use strict";

const {
  Object, Function, Symbol, TypeError,
} = global;

const Object_defineProperty = Object.defineProperty,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty);

const {
  create: createSym,
} = Symbol;

// pseudo-symbol in SpiderMonkey
const mozIteratorSym = "@@iterator";

// create overrides for Map/Set/WeakMap/WeakSet
// - to enable construction without `new`
// - to enable initialisation with `mozIteratorSym`

{ /* Map */
  const BuiltinMap = global.Map;
  const isMapSym = Symbol("isMap");

  class Map extends BuiltinMap {
    constructor(iterable) {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new Map(iterable);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isMapSym) || this[isMapSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isMapSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
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
      var m = super();
      Object_defineProperty(m, isMapSym, {__proto__: null, value: false, configurable: true});
      return m;
    }
  }

  Object.defineProperties(Map.prototype, {
    set: {enumerable: false},
    size: {enumerable: false},
  });

  Object.defineProperties(Map, {
    [createSym]: {writable: false, enumerable: false},
  });

  global.Map = Map;
}

{ /* Set */
  const BuiltinSet = global.Set;
  const isSetSym = Symbol("isSet");

  class Set extends BuiltinSet {
    constructor(iterable) {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new Set(iterable);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isSetSym) || this[isSetSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isSetSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
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
      var m = super();
      Object_defineProperty(m, isSetSym, {__proto__: null, value: false, configurable: true});
      return m;
    }
  }

  Object.defineProperties(Set.prototype, {
    add: {enumerable: false},
    size: {enumerable: false},
  });

  Object.defineProperties(Set, {
    [createSym]: {writable: false, enumerable: false},
  });

  global.Set = Set;
}

{ /* WeakMap */
  const BuiltinWeakMap = global.WeakMap;
  const isWeakMapSym = Symbol("isWeakMap");

  class WeakMap extends BuiltinWeakMap {
    constructor(iterable) {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new WeakMap(iterable);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isWeakMapSym) || this[isWeakMapSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isWeakMapSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
        iterable = iterable[mozIteratorSym]();
      }
      return super(iterable);
    }

    get(key, defaultValue) {
      return this.has(key) ? super(key) : defaultValue;
    }

    // overridden to change return value to `undefined`
    set(key, value) {
      super(key, value);
    }

    static [createSym]() {
      var m = super();
      Object_defineProperty(m, isWeakMapSym, {__proto__: null, value: false, configurable: true});
      return m;
    }
  }

  Object.defineProperties(WeakMap.prototype, {
    get: {enumerable: false},
    set: {enumerable: false},
  });

  Object.defineProperties(WeakMap, {
    [createSym]: {writable: false, enumerable: false},
  });

  global.WeakMap = WeakMap;
}

{ /* WeakSet */
  const BuiltinWeakSet = global.WeakSet;
  const isWeakSetSym = Symbol("isWeakSet");

  class WeakSet extends BuiltinWeakSet {
    constructor(iterable) {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new WeakSet(iterable);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isWeakSetSym) || this[isWeakSetSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isWeakSetSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
        iterable = iterable[mozIteratorSym]();
      }
      return super(iterable);
    }

    // overridden to change return value to `undefined`
    add(value) {
      super(value);
    }

    static [createSym]() {
      var m = super();
      Object_defineProperty(m, isWeakSetSym, {__proto__: null, value: false, configurable: true});
      return m;
    }
  }

  Object.defineProperties(WeakSet.prototype, {
    add: {enumerable: false},
  });

  Object.defineProperties(WeakSet, {
    [createSym]: {writable: false, enumerable: false},
  });

  global.WeakSet = WeakSet;
}

})(this);
