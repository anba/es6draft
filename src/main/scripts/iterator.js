/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function IteratorAPI() {
"use strict";

const global = %GlobalObject();

const {
  Object, Function, Array, String, Symbol, TypeError, Proxy, Reflect
} = global;

const Object_keys = Object.keys,
      Object_defineProperty = Object.defineProperty,
      Object_prototype_hasOwnProperty = Object.prototype.hasOwnProperty;

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
  get() {
    return this[mozIteratorSym];
  },
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

// TODO: Add intrinsic for `StopIteration`?
// Export as global object
Object.defineProperty(global, "StopIteration", {
  value: StopIteration,
  writable: true, enumerable: false, configurable: true
});

// Iterator object
const Iterator = MakeIterator();
function MakeIterator() {
  const nextSym = Symbol("next");

  function mixin(target, source) {
    for (let name of Reflect.ownKeys(source)) {
      Reflect.defineProperty(target, name, Reflect.getOwnPropertyDescriptor(source, name));
    }
    return target;
  }

  function ToIteratorKey(name) {
    let int32 = name | 0;
    if (int32 >= 0 && int32 <= 0x7fffffff && int32 + "" === name) {
      return int32;
    }
    return name;
  }

  class KeyIterator extends null {
    constructor(o) {
      this._keys = Object_keys(o);
      this._index = 0;
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
      this._object = Object(o);
      this._keys = Object_keys(o);
      this._index = 0;
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

  function IsIterator(o) {
    return Object(o) === o && %CallFunction(Object_prototype_hasOwnProperty, o, nextSym);
  }

  function Iterator(obj, keys) {
    if (IsIterator(this) && this[nextSym] === null) {
      return ToIterator(this, obj, keys);
    } else {
      return new Iterator(obj, keys);
    }
  }
  Iterator.prototype = ToIterator(Object.create(Object.prototype), []);

  Object.defineProperties(mixin(Iterator, {
    [createSym]() {
      var o = Object.create(Iterator.prototype);
      Object_defineProperty(o, nextSym, {__proto__: null, value: null, configurable: true});
      return o;
    }
  }), {
    [createSym]: {writable: false, enumerable: false},
  });

  Object.defineProperties(mixin(Iterator.prototype, {
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
      return %CallFunction(Function.prototype[createSym], this);
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

  class BuiltinIterator extends Iterator {
    constructor(obj, iterF) {
      Object_defineProperty(this, iterSym, {__proto__: null, value: %CallFunction(iterF, obj)});
    }

    next() {
      if (this === BuiltinIterator.prototype) {
        return {value: void 0, done: true};
      }
      return this[iterSym].next();
    }

    get [toStringTagSym]() {
      return ctor.name + " Iterator";
    }

    [mozIteratorSym]() {
      return this;
    }

    static [createSym]() {
      return %CallFunction(Function.prototype[createSym], this);
    }
  }

  delete BuiltinIterator.prototype.constructor;

  Object.defineProperties(BuiltinIterator.prototype, {
    next: {enumerable: false},
    [toStringTagSym]: {enumerable: false},
    [mozIteratorSym]: {enumerable: false},
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
  for (const type of ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"]) {
    const ctor = global[`${type}Array`];

    // "@@iterator" iterator based on Array.prototype[mozIteratorSym]
    Object.defineProperties(Object.assign(ctor.prototype, {
      [mozIteratorSym]() {
        return %CallFunction(ArrayPrototype_iterator, this);
      }
    }), {
      [mozIteratorSym]: {enumerable: false},
    });
  }
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

})();
