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

const iteratorSym = getSym("@@iterator");
const nextSym = newSym("next");

Object.defineProperty(Object.prototype, iteratorSym, {
  get() { return this.iterator }
});

function ToIterator(instance, obj, keys) {
  var iter = (
    Array_isArray(obj) && keys ? obj.map((_, k) => k) :
    Array_isArray(obj) ? obj.map((v, k) => [k, v]) :
    keys ? Object_keys(Object(obj)) :
    Object_keys(Object(obj)).map(k => [k, obj[k]])
  ).values();
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
Object.defineProperty(global, "Iterator", {
  value: Iterator,
  writable: true, enumerable: false, configurable: true
});

Object.defineProperty(Iterator, getSym("@@create"), {
  value: function() {
    var o = Object.create(Iterator.prototype);
    Object_defineProperty(o, nextSym, {value: null, configurable: true});
    return o;
  }
});

Iterator.prototype = ToIterator(Object.create(Object.prototype), []);

Object.defineProperties(Object.assign(Iterator.prototype, {
  iterator() {
    return this;
  },
  next() {
    if (!IsIterator(this)) {
      throw TypeError();
    }
    return this[nextSym]();
  }
}), {
  iterator: {enumerable: false},
  next: {enumerable: false},
});

// adjust prototype chain for built-in iterators
[[], new Map, new Set].forEach(v => v.values().__proto__.__proto__ = Iterator.prototype);

// make prototype.iterator() an own data property and remove @@iterator hook
[Map, Set].forEach(
  ctor => {
    Object.defineProperty(ctor.prototype, "iterator", {
      value: ctor.prototype[iteratorSym],
      writable: true, enumerable: false, configurable: true
    });
    delete ctor.prototype[iteratorSym];
  }
);

const ArrayPrototype_iterator = (function() {
  const arrayIterator = Array.prototype[iteratorSym];
  const throwsOnGet = new Proxy({}, {get: () => { throw TypeError() }});
  return {
    iterator() {
      return arrayIterator.call(this != null ? this : throwsOnGet);
    }
  }.iterator;
})();

// remove @@iterator on Array.prototype
delete Array.prototype[iteratorSym];

const TypedArrays = [Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array, Float32Array, Float64Array];

// make Strings and TypedArrays iterable
[Array, String, ...TypedArrays].forEach(
  ctor => {
    Object.defineProperty(ctor.prototype, "iterator", {
      value() { return ArrayPrototype_iterator.call(this) },
      writable: true, enumerable: false, configurable: true
    });
  }
);

})(this);
