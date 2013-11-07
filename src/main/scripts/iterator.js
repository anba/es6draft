/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function IteratorAPI(global) {
"use strict";

const {
  Object, Function, Array, String, Symbol, TypeError, Proxy,
} = global;

const Object_keys = Object.keys,
      Object_defineProperty = Object.defineProperty,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty),
      Array_isArray = Array.isArray;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const {
  iterator: iteratorSym,
  toStringTag: toStringTagSym,
  hasInstance: hasInstanceSym,
  create: createSym,
} = Symbol;

// pseudo-symbol in SpiderMonkey
const mozIteratorSym = "@@iterator";

// map from Symbol.iterator to pseudo-symbol "@@iterator"
Object.defineProperty(Object.prototype, iteratorSym, {
  get() { return () => this[mozIteratorSym]() },
  set(iter) {
    Object_defineProperty(this, iteratorSym, {
      __proto__: null,
      value: iter,
      writable: true, enumerable: true, configurable: true
    });
  },
  enumerable: false, configurable: true
});

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
  const nextSym = Symbol("next");

  function ToIterator(instance, obj, keys = false) {
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
  Iterator.prototype = ToIterator(Object.create(Object.prototype), []);

  Object.defineProperties(Object.mixin(Iterator, {
    [createSym]() {
      var o = Object.create(Iterator.prototype);
      Object_defineProperty(o, nextSym, {__proto__: null, value: null, configurable: true});
      return o;
    }
  }), {
    [createSym]: {writable: false, enumerable: false},
  });

  Object.defineProperties(Object.mixin(Iterator.prototype, {
    constructor: Iterator,
    get [toStringTagSym]() {
      return "Iterator";
    },
    [iteratorSym]() {
      return this[mozIteratorSym]();
    },
    [mozIteratorSym]() {
      return new LegacyIterator(this);
    },
    next() {
      if (!IsIterator(this)) {
        throw new TypeError();
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
    constructor: {enumerable: false},
    [toStringTagSym]: {enumerable: false},
    [iteratorSym]: {enumerable: false},
    [mozIteratorSym]: {enumerable: false},
    next: {enumerable: false},
  });

  return Iterator;
}

// export as global constructor
Object.defineProperty(global, "Iterator", {
  value: Iterator,
  writable: true, enumerable: false, configurable: true
});

// (internal) LegacyIterator object
const LegacyIterator = MakeLegacyIterator();
function MakeLegacyIterator() {
  const iterSym = Symbol("iter");

  class LegacyIterator extends Iterator {
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

    [mozIteratorSym]() {
      return this;
    }

    static [createSym]() {
      return $CallFunction(Function.prototype[createSym], this);
    }
  }

  delete LegacyIterator.prototype.constructor;

  Object.defineProperties(LegacyIterator.prototype, {
    next: {enumerable: false},
    [mozIteratorSym]: {enumerable: false},
  });

  Object.defineProperties(LegacyIterator, {
    [createSym]: {writable: false, enumerable: false},
  });

  return LegacyIterator;
}

// (internal) BuiltinIterator object
function MakeBuiltinIterator(ctor) {
  const iterSym = Symbol("iter");
  const closedSym = Symbol("closed");

  class BuiltinIterator extends Iterator {
    constructor(obj, iterF) {
      Object_defineProperty(this, iterSym, {__proto__: null, value: $CallFunction(iterF, obj)});
      Object_defineProperty(this, closedSym, {__proto__: null, value: false, configurable: true});
    }

    next() {
      if (!this[closedSym]) {
        var next = this[iterSym].next();
        if (Object(next) === next && next.done) {
          Object_defineProperty(this, closedSym, {__proto__: null, value: true, configurable: false});
        }
        return next;
      }
      return {value: void 0, done: true};
    }

    get [toStringTagSym]() {
      return ctor.name + " Iterator";
    }

    [mozIteratorSym]() {
      return this;
    }

    get [closedSym]() {
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
    [mozIteratorSym]: {enumerable: false},
    [closedSym]: {enumerable: false},
  });

  Object.defineProperties(BuiltinIterator, {
    [createSym]: {writable: false, enumerable: false},
  });

  return BuiltinIterator;
}

// make prototype[mozIteratorSym]() an own data property and remove @@iterator hook

{ /* Map.prototype */
  const Map = global.Map;
  const BuiltinIterator = MakeBuiltinIterator(Map);
  const {
    keys: Map_prototype_keys,
    values: Map_prototype_values,
    entries: Map_prototype_entries,
  } = Map.prototype;

  Object.defineProperties(Object.assign(Map.prototype, {
    keys() { return new BuiltinIterator(this, Map_prototype_keys) },
    values() { return new BuiltinIterator(this, Map_prototype_values) },
    entries() { return new BuiltinIterator(this, Map_prototype_entries) },
  }), {
    keys: {enumerable: false},
    values: {enumerable: false},
    entries: {enumerable: false},
  });

  // Map.prototype[mozIteratorSym] === Map.prototype.entries
  Object.defineProperty(Map.prototype, mozIteratorSym, {
    value: Map.prototype.entries,
    writable: true, enumerable: false, configurable: true
  });

  // delete original Map.prototype[@@iterator]
  delete Map.prototype[iteratorSym];
}

{ /* Set.prototype */
  const Set = global.Set;
  const BuiltinIterator = MakeBuiltinIterator(Set);
  const {
    values: Set_prototype_values,
    entries: Set_prototype_entries,
  } = Set.prototype;

  Object.defineProperties(Object.assign(Set.prototype, {
    values() { return new BuiltinIterator(this, Set_prototype_values) },
    entries() { return new BuiltinIterator(this, Set_prototype_entries) },
  }), {
    values: {enumerable: false},
    entries: {enumerable: false},
  });

  // Set.prototype.keys === Set.prototype.values
  Object.defineProperty(Set.prototype, "keys", {
    value: Set.prototype.values,
    writable: true, enumerable: false, configurable: true
  });

  // Set.prototype[mozIteratorSym] === Set.prototype.values
  Object.defineProperty(Set.prototype, mozIteratorSym, {
    value: Set.prototype.values,
    writable: true, enumerable: false, configurable: true
  });

  // delete original Set.prototype[@@iterator]
  delete Set.prototype[iteratorSym];
}

{ /* Array.prototype */
  const BuiltinIterator = MakeBuiltinIterator(Array);
  const {
    keys: Array_prototype_keys,
    values: Array_prototype_values,
    entries: Array_prototype_entries,
  } = Array.prototype;

  // Array.prototype[mozIteratorSym]
  Object.defineProperties(Object.assign(Array.prototype, {
    [mozIteratorSym]() {
      return new BuiltinIterator(this, Array_prototype_values);
    },
    keys() {
      return new BuiltinIterator(this, Array_prototype_keys);
    },
    entries() {
      return new BuiltinIterator(this, Array_prototype_entries);
    },
  }), {
    [mozIteratorSym]: {enumerable: false},
    keys: {enumerable: false},
    entries: {enumerable: false},
  });

  // delete original Array.prototype[@@iterator]
  delete Array.prototype[iteratorSym];

  // values currently disabled in SpiderMonkey :(
  delete Array.prototype.values;
}

// make TypedArrays iterable
{
  const ArrayPrototype_iterator = Array.prototype[mozIteratorSym];
  const types = ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"];
  const TypedArrays = [for (type of types) global[type + "Array"]];
  [String, ...TypedArrays].forEach(
    ctor => {
      // "@@iterator" iterator based on Array.prototype[mozIteratorSym]
      Object.defineProperties(Object.assign(ctor.prototype, {
        [mozIteratorSym]() {
          return $CallFunction(ArrayPrototype_iterator, this);
        }
      }), {
        [mozIteratorSym]: {enumerable: false},
      });
    }
  );
}

{
  const StringIteratorPrototype = Object.getPrototypeOf(""[iteratorSym]());

  // add StringIterator.prototype[mozIteratorSym]
  Object.defineProperty(StringIteratorPrototype, mozIteratorSym, {
    value: StringIteratorPrototype[iteratorSym],
    writable: true, enumerable: false, configurable: true
  });

  // add String.prototype[mozIteratorSym]
  Object.defineProperty(String.prototype, mozIteratorSym, {
    value: String.prototype[iteratorSym],
    writable: true, enumerable: false, configurable: true
  });

  // change "length" to non-configurable for my own tests in SpiderMonkey... :-/
  Object.defineProperty(StringIteratorPrototype[mozIteratorSym], "length", {
    configurable: false
  });
  Object.defineProperty(StringIteratorPrototype.next, "length", {
    configurable: false
  });
  Object.defineProperty(String.prototype[mozIteratorSym], "length", {
    configurable: false
  });

  // delete original StringIteratorPrototype[@@iterator] and String.prototype[@@iterator]
  delete StringIteratorPrototype[iteratorSym];
  delete String.prototype[iteratorSym];
}

// create overrides for Map/Set/WeakMap/WeakSet

{ /* Map */
  const BuiltinMap = global.Map;
  const isMapSym = Symbol("isMap");

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
        iterable = iterable[mozIteratorSym]();
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
  const isSetSym = Symbol("isSet");

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
        iterable = iterable[mozIteratorSym]();
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
  const isWeakMapSym = Symbol("isWeakMap");

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
          iterable = iterable.entries();
        } else {
          iterable = iterable[mozIteratorSym]();
        }
      }
      return super(iterable, comparator);
    }

    get(key, defaultValue) {
      return this.has(key) ? super(key) : defaultValue;
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
  const isWeakSetSym = Symbol("isWeakSet");

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
        iterable = iterable[mozIteratorSym]();
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
