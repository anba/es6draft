/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function IteratorAPI(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      Array = global.Array,
      String = global.String,
      TypeError = global.TypeError,
      Proxy = global.Proxy;

const Object_keys = Object.keys,
      Object_defineProperty = Object.defineProperty,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty),
      Array_isArray = Array.isArray;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const iteratorSym = getSym("@@iterator"),
      toStringTagSym = getSym("@@toStringTag"),
      hasInstanceSym = getSym("@@hasInstance"),
      createSym = getSym("@@create");


// StopIteration object
const StopIteration = Object.freeze(Object.defineProperties({
  [toStringTagSym]: "StopIteration",
  [hasInstanceSym]: o => (o === StopIteration),
}, {
  [toStringTagSym]: {writable: false, enumerable: false},
  [hasInstanceSym]: {writable: false, enumerable: false},
}));

Object.defineProperty(global, "StopIteration", {
  value: StopIteration,
  writable: true, enumerable: false, configurable: true
});

// Iterator object
const Iterator = MakeIterator();
function MakeIterator() {
  const nextSym = newSym("next");

  function ToIterator(instance, obj, keys) {
    var iter = (
      Array_isArray(obj) && keys ? obj.map((_, k) => k) :
      Array_isArray(obj) ? obj.map((v, k) => [k, v]) :
      keys ? Object_keys(Object(obj)) :
      Object_keys(Object(obj)).map(k => [k, obj[k]])
    )[iteratorSym]();
    var next = iter.next.bind(iter);
    Object_defineProperty(instance, nextSym, {__proto__: null, value: next, configurable: false});
    return new Proxy(instance, {enumerate: () => iter});
  }

  function IsIterator(o) {
    return Object(o) === o && Object_hasOwnProperty(o, nextSym);
  }

  function Iterator(obj, keys) {
    if (IsIterator(this) && this[nextSym] === null) {
      return ToIterator(this, obj, keys);
    } else {
      return new Iterator(obj, keys);
    }
  }

  Object.defineProperty(Iterator, createSym, {
    value: function() {
      var o = Object.create(Iterator.prototype);
      Object_defineProperty(o, nextSym, {__proto__: null, value: null, configurable: true});
      return o;
    }, writable: false, enumerable: false, configurable: true
  });

  Iterator.prototype = ToIterator(Object.create(Object.prototype), []);
  Iterator.prototype.constructor = Iterator;

  Object.defineProperties(Object.assign(Iterator.prototype, {
    iterator() {
      return this;
    },
    next() {
      if (!IsIterator(this)) {
        throw TypeError();
      }
      var next = this[nextSym]();
      if (Object(next) === next) {
        if (next.done) {
          throw StopIteration;
        }
        return next.value;
      }
    }
  }), {
    iterator: {enumerable: false},
    next: {enumerable: false},
  });

  return Iterator;
}

Object.defineProperty(global, "Iterator", {
  value: Iterator,
  writable: true, enumerable: false, configurable: true
});

// (internal) IteratorAdapter object
const IteratorAdapter = MakeIteratorAdapter();
function MakeIteratorAdapter() {
  const iterSym = newSym("iter");

  class IteratorAdapter {
    constructor(iter) {
      Object_defineProperty(this, iterSym, {__proto__: null, value: iter});
    }

    next() {
      try {
        var value = this[iterSym].next();
        return {value, done: false};
      } catch (e) {
        if (e === StopIteration) {
          return {value, done: true};
        }
        throw e;
      }
    }

    iterator() {
      return this[iterSym];
    }

    [iteratorSym]() {
      return this;
    }
  }

  Object.defineProperties(IteratorAdapter.prototype, {
    next: {enumerable: false},
    iterator: {enumerable: false},
    [iteratorSym]: {writable: false, enumerable: false},
  });

  return IteratorAdapter;
}

Object.defineProperty(Object.prototype, iteratorSym, {
  get() { return () => new IteratorAdapter(this.iterator()) },
  enumerable: false, configurable: true
});

// (internal) MakeBuiltinIterator
function MakeBuiltinIterator(ctor) {
  const iterSym = newSym("iter");
  const drainedSym = newSym("drained");

  class BuiltinIterator extends Iterator {
    constructor(obj, iterF) {
      Object_defineProperty(this, iterSym, {__proto__: null, value: $CallFunction(iterF, obj)});
      Object_defineProperty(this, drainedSym, {__proto__: null, value: false, configurable: true});
    }

    next() {
      if (!this[drainedSym]) {
        var next = this[iterSym].next();
        if (Object(next) === next && !next.done) {
          return next.value;
        }
        Object_defineProperty(this, drainedSym, {__proto__: null, value: true, configurable: false});
      }
      throw StopIteration;
    }

    get [toStringTagSym]() {
      return ctor.name + " Iterator";
    }

    [iteratorSym]() {
      return this[iterSym];
    }

    get [drainedSym]() {
      return true;
    }

    static [createSym]() {
      return $CallFunction(Function.prototype[createSym], this);
    }
  }

  delete BuiltinIterator.prototype.constructor;

  Object.defineProperties(BuiltinIterator.prototype, {
    next: {enumerable: false},
    [toStringTagSym]: {enumerable: false},
    [iteratorSym]: {writable: false, enumerable: false},
    [drainedSym]: {enumerable: false},
  });

  Object.defineProperties(BuiltinIterator, {
    [createSym]: {writable: false, enumerable: false},
  });

  return BuiltinIterator;
}

// make prototype.iterator() an own data property and remove @@iterator hook

{ /* Map.prototype */
  const Map = global.Map;
  const BuiltinIterator = MakeBuiltinIterator(Map);
  const iterF = {
    keys: Map.prototype['keys'],
    values: Map.prototype['values'],
    entries: Map.prototype['entries'],
  };

  Object.defineProperties(Object.assign(Map.prototype, {
    keys() { return new BuiltinIterator(this, iterF.keys) },
    values() { return new BuiltinIterator(this, iterF.values) },
    entries() { return new BuiltinIterator(this, iterF.entries) },
  }), {
    keys: {enumerable: false},
    values: {enumerable: false},
    entries: {enumerable: false},
  });

  // Map.prototype.iterator === Map.prototype.entries
  Object.defineProperty(Map.prototype, "iterator", {
    value: Map.prototype.entries,
    writable: true, enumerable: false, configurable: true
  });

  delete Map.prototype[iteratorSym];
}

{ /* Set.prototype */
  const Set = global.Set;
  const BuiltinIterator = MakeBuiltinIterator(Set);
  const iterF = {
    values: Set.prototype['values'],
    entries: Set.prototype['entries'],
  };

  Object.defineProperties(Object.assign(Set.prototype, {
    values() { return new BuiltinIterator(this, iterF.values) },
    entries() { return new BuiltinIterator(this, iterF.entries) },
  }), {
    values: {enumerable: false},
    entries: {enumerable: false},
  });

  // Set.prototype.keys === Set.prototype.values
  Object.defineProperty(Set.prototype, "keys", {
    value: Set.prototype.values,
    writable: true, enumerable: false, configurable: true
  });

  // Set.prototype.iterator === Set.prototype.values
  Object.defineProperty(Set.prototype, "iterator", {
    value: Set.prototype.values,
    writable: true, enumerable: false, configurable: true
  });

  delete Set.prototype[iteratorSym];
}

{ /* Array.prototype */
  const BuiltinIterator = MakeBuiltinIterator(Array);
  const iterF = {
    keys: Array.prototype['keys'],
    values: Array.prototype['values'],
    entries: Array.prototype['entries'],
  };
  const throwsOnGet = new Proxy({}, {get: () => { throw TypeError() }});

  Object.defineProperties(Object.assign(Array.prototype, {
    keys() {
      return new BuiltinIterator(this != null ? this : throwsOnGet, iterF.keys)
    },
    values() {
      return new BuiltinIterator(this != null ? this : throwsOnGet, iterF.values)
    },
    entries() {
      return new BuiltinIterator(this != null ? this : throwsOnGet, iterF.entries)
    },
  }), {
    keys: {enumerable: false},
    values: {enumerable: false},
    entries: {enumerable: false},
  });

  // Array.prototype.iterator === Array.prototype.values
  Object.defineProperty(Array.prototype, "iterator", {
    value: Array.prototype.values,
    writable: true, enumerable: false, configurable: true
  });

  delete Array.prototype[iteratorSym];

  // keys, values and entries currently disabled in SpiderMonkey :(
  delete Array.prototype.keys;
  delete Array.prototype.values;
  delete Array.prototype.entries;
}

// make Strings and TypedArrays iterable
{
  const ArrayPrototype_iterator = Array.prototype.iterator;
  const types = ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"];
  const TypedArrays = [for (type of types) global[type + "Array"]];
  [String, ...TypedArrays].forEach(
    ctor => {
      Object.defineProperty(ctor.prototype, "iterator", {
        value() { return $CallFunction(ArrayPrototype_iterator, this) },
        writable: true, enumerable: false, configurable: true
      });
    }
  );
}

// create overrides for Map/Set/WeakMap/WeakSet

{ /* Map */
  const BuiltinMap = global.Map;
  const isMapSym = newSym("isMap");

  class Map extends BuiltinMap {
    constructor(iterable, comparator = "is") {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new Map(iterable, comparator);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isMapSym) || this[isMapSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isMapSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
        if ("entries" in iterable) {
          iterable = iterable.entries()[iteratorSym]();
        } else {
          iterable = iterable.iterator()[iteratorSym]();
        }
      }
      return super(iterable, comparator);
    }

    set(key, value) {
      super(key, value);
    }

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

  Object.defineProperty(global, "Map", {
    value: Map,
    writable: true, enumerable: false, configurable: true
  });
}

{ /* Set */
  const BuiltinSet = global.Set;
  const isSetSym = newSym("isSet");

  class Set extends BuiltinSet {
    constructor(iterable, comparator = "is") {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new Set(iterable, comparator);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isSetSym) || this[isSetSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isSetSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
        iterable = iterable.iterator()[iteratorSym]();
      }
      return super(iterable, comparator);
    }

    add(value) {
      super(value);
    }

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

  Object.defineProperty(global, "Set", {
    value: Set,
    writable: true, enumerable: false, configurable: true
  });
}

{ /* WeakMap */
  const BuiltinWeakMap = global.WeakMap;
  const isWeakMapSym = newSym("isWeakMap");

  class WeakMap extends BuiltinWeakMap {
    constructor(iterable, comparator = undefined) {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new WeakMap(iterable, comparator);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isWeakMapSym) || this[isWeakMapSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isWeakMapSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
        if ("entries" in iterable) {
          iterable = iterable.entries()[iteratorSym]();
        } else {
          iterable = iterable.iterator()[iteratorSym]();
        }
      }
      return super(iterable, comparator);
    }

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
    set: {enumerable: false},
  });

  Object.defineProperties(WeakMap, {
    [createSym]: {writable: false, enumerable: false},
  });

  Object.defineProperty(global, "WeakMap", {
    value: WeakMap,
    writable: true, enumerable: false, configurable: true
  });
}

{ /* WeakSet */
  const BuiltinWeakSet = global.WeakSet;
  const isWeakSetSym = newSym("isWeakSet");

  class WeakSet extends BuiltinWeakSet {
    constructor(iterable, comparator = undefined) {
      if (!(typeof this == 'object' && this !== null)) {
        if (this === undefined) {
          return new WeakSet(iterable, comparator);
        }
        throw new TypeError();
      }
      if (!Object_hasOwnProperty(this, isWeakSetSym) || this[isWeakSetSym] !== false) {
        throw new TypeError();
      }
      Object_defineProperty(this, isWeakSetSym, {__proto__: null, value: true, configurable: false});
      if (iterable !== undefined) {
        iterable = iterable.iterator()[iteratorSym]();
      }
      return super(iterable, comparator);
    }

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

  Object.defineProperty(global, "WeakSet", {
    value: WeakSet,
    writable: true, enumerable: false, configurable: true
  });
}

})(this);
