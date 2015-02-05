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
  Object, Function, Array, String, Symbol, TypeError, Proxy, Reflect, Int8Array
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

// (internal) BuiltinIterator object
function MakeBuiltinIterator(tag, throwErrorIfPrototype) {
  const iterSym = Symbol.for(`std::Builtin${tag}Iterator::iterator`);

  class BuiltinIterator {
    constructor(obj, iterF) {
      Object_defineProperty(this, iterSym, {__proto__: null, value: %CallFunction(iterF, obj)});
    }

    next() {
      if (this === BuiltinIterator.prototype) {
        if (throwErrorIfPrototype) {
          // Tests require this exact error message to be thrown! :-/
          throw TypeError(`Call${tag}IteratorMethodIfWrapped method called on incompatible ${tag} Iterator`);
        }
        return {value: void 0, done: true};
      }
      return this[iterSym].next();
    }

    get [toStringTagSym]() {
      return `${tag} Iterator`;
    }

    [iteratorSym]() {
      return this;
    }
  }

  delete BuiltinIterator.prototype.constructor;

  Object.setPrototypeOf(BuiltinIterator, Iterator);
  Object.setPrototypeOf(BuiltinIterator.prototype, Iterator.prototype);

  return BuiltinIterator;
}

{ /* Map.prototype */
  const Map = global.Map;
  const BuiltinIterator = MakeBuiltinIterator("Map", false);
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

  // Map.prototype[iteratorSym] === Map.prototype.entries
  Object.defineProperty(Map.prototype, iteratorSym, {
    value: Map.prototype.entries,
    writable: true, enumerable: false, configurable: true
  });
}

{ /* Set.prototype */
  const Set = global.Set;
  const BuiltinIterator = MakeBuiltinIterator("Set", false);
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

  // Set.prototype[iteratorSym] === Set.prototype.values
  Object.defineProperty(Set.prototype, iteratorSym, {
    value: Set.prototype.values,
    writable: true, enumerable: false, configurable: true
  });
}

{ /* Array.prototype */
  const BuiltinIterator = MakeBuiltinIterator("Array", true);
  const {
    keys: Array_prototype_keys,
    values: Array_prototype_values,
    entries: Array_prototype_entries,
  } = Array.prototype;

  Object.defineProperties(Object.assign(Array.prototype, {
    [iteratorSym]() {
      return new BuiltinIterator(this, Array_prototype_values);
    },
    keys() {
      return new BuiltinIterator(this, Array_prototype_keys);
    },
    entries() {
      return new BuiltinIterator(this, Array_prototype_entries);
    },
  }), {
    [iteratorSym]: {enumerable: false},
    keys: {enumerable: false},
    entries: {enumerable: false},
  });

  // values currently disabled in SpiderMonkey :(
  delete Array.prototype.values;

  const TypedArray = Object.getPrototypeOf(Int8Array);
  const {
    keys: TypedArray_prototype_keys,
    entries: TypedArray_prototype_entries,
    values: TypedArray_prototype_values,
  } = TypedArray.prototype;

  Object.defineProperties(Object.assign(TypedArray.prototype, {
    keys() {
      return new BuiltinIterator(this, TypedArray_prototype_keys);
    },
    entries() {
      return new BuiltinIterator(this, TypedArray_prototype_entries);
    },
    values() {
      return new BuiltinIterator(this, TypedArray_prototype_values);
    },
  }), {
    values: {enumerable: false},
    keys: {enumerable: false},
    entries: {enumerable: false},
  });

  // share "@@iterator" with TypedArray.prototype
  Object.defineProperty(TypedArray.prototype, iteratorSym, {
    value: TypedArray.prototype.values,
    writable: true, enumerable: false, configurable: true
  });
}

{
  const BuiltinIterator = MakeBuiltinIterator("String", true);
  const {
    [iteratorSym]: String_prototype_iterator,
  } = String.prototype;

  Object.defineProperties(Object.assign(String.prototype, {
    [iteratorSym]() {
      return new BuiltinIterator(this, String_prototype_iterator);
    },
  }), {
    [iteratorSym]: {enumerable: false},
  });

  // const StringIteratorPrototype = Object.getPrototypeOf(""[iteratorSym]());
  const StringIteratorPrototype = BuiltinIterator.prototype;

  // change "length" to non-configurable for my own tests in SpiderMonkey... :-/
  Object.defineProperty(StringIteratorPrototype.next, "length", {
    configurable: false
  });
}

})();
