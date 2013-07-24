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
    ).values()[iteratorSym]();
    var next = iter.next.bind(iter);
    Object_defineProperty(instance, nextSym, {value: next, configurable: false});
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
      Object_defineProperty(o, nextSym, {value: null, configurable: true});
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
      Object.defineProperty(this, iterSym, {value: iter});
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

  class BuiltinIterator extends Iterator {
    constructor(obj, iterF) {
      Object.defineProperty(this, iterSym, {value: iterF.call(obj)});
    }

    next() {
      var next = this[iterSym].next();
      if (Object(next) === next) {
        if (next.done) {
          throw StopIteration;
        }
        return next.value;
      }
    }

    iterator() {
      return this;
    }

    get [toStringTagSym]() {
      return ctor.name + " Iterator";
    }

    [iteratorSym]() {
      return this[iterSym];
    }

    static [createSym]() {
      return Function.prototype[createSym].call(this);
    }
  }

  delete BuiltinIterator.prototype.constructor;

  Object.defineProperties(BuiltinIterator.prototype, {
    next: {enumerable: false},
    iterator: {enumerable: false},
    [toStringTagSym]: {enumerable: false},
    [iteratorSym]: {writable: false, enumerable: false},
  });

  Object.defineProperties(BuiltinIterator, {
    [createSym]: {writable: false, enumerable: false},
  });

  return BuiltinIterator;
}

// make prototype.iterator() an own data property and remove @@iterator hook

{ /* Map.prototype */
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
}

// make Strings and TypedArrays iterable
{
  const ArrayPrototype_iterator = Array.prototype.iterator;
  const types = ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"];
  const TypedArrays = [for (type of types) global[type + "Array"]];
  [String, ...TypedArrays].forEach(
    ctor => {
      Object.defineProperty(ctor.prototype, "iterator", {
        value() { return ArrayPrototype_iterator.call(this) },
        writable: true, enumerable: false, configurable: true
      });
    }
  );
}

})(this);
