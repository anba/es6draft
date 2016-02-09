/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function IteratorAPI() {
"use strict";

const global = %GlobalObject();

const {
  Object, Symbol, TypeError, Proxy
} = global;

const {
  create: Object_create,
  keys: Object_keys,
  defineProperty: Object_defineProperty,
  prototype: {
    hasOwnProperty: Object_prototype_hasOwnProperty,
    isPrototypeOf: Object_prototype_isPrototypeOf,
  }
} = Object;

const {
  iterator: iteratorSym,
  toStringTag: toStringTagSym,
  hasInstance: hasInstanceSym,
} = Symbol;

// StopIteration object
const StopIteration = Object.freeze(Object.defineProperties({
  [toStringTagSym]: "StopIteration",
  [hasInstanceSym]: o => (o === StopIteration),
}, {
  [toStringTagSym]: {writable: false, enumerable: false},
  [hasInstanceSym]: {writable: false, enumerable: false},
}));

// TODO: Add intrinsic for `StopIteration`?
// Export as global object
Object.defineProperty(global, "StopIteration", {
  value: StopIteration,
  writable: true, enumerable: false, configurable: true
});

// Iterator object
const Iterator = MakeIterator();
function MakeIterator() {
  const nextSym = Symbol.for(`std::BuiltinIterator::next`);

  function ToIteratorKey(name) {
    let int32 = name | 0;
    if (int32 >= 0 && int32 <= 0x7fffffff && int32 + "" === name) {
      return int32;
    }
    return name;
  }

  class KeyIterator extends null {
    constructor(o) {
      var obj = Object_create(new.target.prototype);
      obj._keys = Object_keys(o);
      obj._index = 0;
      return obj;
    }

    next() {
      let value, done = (this._index >= this._keys.length);
      if (!done) {
        value = ToIteratorKey(this._keys[this._index++]);
      }
      return {value, done};
    }
  }

  class KeyValueIterator extends null {
    constructor(o) {
      var obj = Object_create(new.target.prototype);
      obj._object = Object(o);
      obj._keys = Object_keys(o);
      obj._index = 0;
      return obj;
    }

    next() {
      let value, done = (this._index >= this._keys.length);
      if (!done) {
        let key = ToIteratorKey(this._keys[this._index++]);
        value = [key, this._object[key]];
      }
      return {value, done};
    }
  }

  function ToIterator(instance, obj, keys = false) {
    var iter = keys ? new KeyIterator(obj) : new KeyValueIterator(obj);
    Object_defineProperty(instance, nextSym, {__proto__: null, value: () => iter.next(), configurable: false});
    return new Proxy(instance, {enumerate: () => iter});
  }

  function IsInitializedIterator(o) {
    return Object(o) === o && %CallFunction(Object_prototype_hasOwnProperty, o, nextSym);
  }

  function Iterator(obj, keys) {
    if (new.target) {
      return ToIterator(this, obj, keys);
    } else {
      return new Iterator(obj, keys);
    }
  }
  Object.assign(Iterator.prototype, {
    next() {
      if (!IsInitializedIterator(this)) {
        throw new TypeError();
      }
      var next = this[nextSym]();
      if (Object(next) === next) {
        if (next.done) {
          throw StopIteration;
        }
        return next.value;
      }
    },
    [iteratorSym]() {
      return new LegacyIterator(this);
    },
    get [toStringTagSym]() {
      return "Iterator";
    },
  });
  Object.defineProperties(Iterator.prototype, {
    constructor: {enumerable: false},
    next: {enumerable: false},
    [iteratorSym]: {enumerable: false},
    [toStringTagSym]: {enumerable: false},
  });
  Iterator.prototype = ToIterator(Iterator.prototype, []);

  return Iterator;
}

// TODO: Add intrinsic for `Iterator`?
// Export as global constructor
Object.defineProperty(global, "Iterator", {
  value: Iterator,
  writable: true, enumerable: false, configurable: true
});

// (internal) LegacyIterator object
const LegacyIterator = MakeLegacyIterator();
function MakeLegacyIterator() {
  const iterSym = Symbol("iter");

  class LegacyIterator {
    constructor(iter) {
      Object_defineProperty(this, iterSym, {__proto__: null, value: iter});
    }

    next(v = void 0) {
      try {
        var value = this[iterSym].next(v);
        return {value, done: false};
      } catch (e) {
        if (e === StopIteration) {
          return {value, done: true};
        }
        throw e;
      }
    }

    [iteratorSym]() {
      return this;
    }
  }

  delete LegacyIterator.prototype.constructor;

  Object.setPrototypeOf(LegacyIterator, Iterator);
  Object.setPrototypeOf(LegacyIterator.prototype, Iterator.prototype);

  return LegacyIterator;
}

})();
