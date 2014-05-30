/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function LegacyGenerator() {
"use strict";

const global = %GlobalObject();

const {
  Object, Function, Symbol, StopIteration, Reflect
} = global;

const {
  defineProperty: Object_defineProperty,
  getPrototypeOf: Object_getPrototypeOf,
  prototype: {
    hasOwnProperty: Object_prototype_hasOwnProperty
  }
} = Object;

const {
  iterator: iteratorSym,
  toStringTag: toStringTagSym,
} = Symbol;

const GeneratorFunction = Object.getPrototypeOf(function*(){}).constructor;
const GeneratorPrototype = Object.getPrototypeOf(function*(){}.prototype);
const GeneratorPrototype_next = GeneratorPrototype.next,
      GeneratorPrototype_throw = GeneratorPrototype.throw;
const LegacyGeneratorPrototype = Object.getPrototypeOf(function(){yield 0}.prototype);

// pseudo-symbol in SpiderMonkey
const mozIteratorSym = "@@iterator";

Object.defineProperties(Object.assign(Function.prototype, {
  isGenerator() {
    return this instanceof GeneratorFunction;
  }
}), {
  isGenerator: {enumerable: false},
});

Object.defineProperties(Object.assign(GeneratorPrototype, {
  [mozIteratorSym]() {
    return this;
  }
}), {
  [mozIteratorSym]: {enumerable: false},
});

//  “newborn”, “executing”, “suspended”, or “closed”
const generatorStateSym = Symbol("generatorState");

function GeneratorState(g) {
  // no proper way to test for generators in scripts
  if (%IsGenerator(g)) {
    if (g === LegacyGeneratorPrototype || Object_getPrototypeOf(g) === LegacyGeneratorPrototype) {
      return;
    }
    if (!%CallFunction(Object_prototype_hasOwnProperty, g, generatorStateSym)) {
      Object_defineProperty(g, generatorStateSym, {__proto__: null, value: "newborn", writable: true});
    }
    return g[generatorStateSym];
  }
}

function GeneratorResume(fn, g, v) {
  g[generatorStateSym] = "executing";

  try {
    var result = %CallFunction(fn, g, v);
  } catch (e) {
    // uncaught exception
    g[generatorStateSym] = "closed";
    throw e;
  }

  if (result.done) {
    g[generatorStateSym] = "closed";
    throw StopIteration;
  } else {
    g[generatorStateSym] = "suspended";
    return result.value;
  }
}

function mixin(target, source) {
  for (let name of Reflect.ownKeys(source)) {
    Reflect.defineProperty(target, name, Reflect.getOwnPropertyDescriptor(source, name));
  }
  return target;
}

Object.defineProperties(mixin(LegacyGeneratorPrototype, {
  [iteratorSym]() {
    return this[mozIteratorSym]();
  },
  [mozIteratorSym]() {
    return {
      [iteratorSym]() {
        return this;
      },
      [mozIteratorSym]() {
        return this;
      },
      next: v => {
        try {
          var value = this.next(v);
          return {value, done: false};
        } catch (e) {
          if (e === StopIteration) {
            return {value, done: true};
          }
          throw e;
        }
      },
      throw: e => {
        try {
          var value = this.throw(e);
          return {value, done: false};
        } catch (e) {
          if (e === StopIteration) {
            return {value, done: true};
          }
          throw e;
        }
      }
    };
  },
  next() {
    switch(GeneratorState(this)) {
      case "newborn":
      case "suspended":
        return GeneratorResume(GeneratorPrototype_next, this);
      case "closed":
        throw StopIteration;
      case "executing":
      default:
        throw TypeError();
    }
  },
  send(v) {
    switch(GeneratorState(this)) {
      case "newborn":
      case "suspended":
        return GeneratorResume(GeneratorPrototype_next, this, v);
      case "closed":
        throw StopIteration;
      case "executing":
      default:
        throw TypeError();
    }
  },
  close() {
    switch(GeneratorState(this)) {
      case "newborn":
      case "suspended":
        this[generatorStateSym] = "closed";
      case "closed":
        return;
      case "executing":
      default:
        throw TypeError();
    }
  },
  throw(e) {
    switch(GeneratorState(this)) {
      case "suspended":
        return GeneratorResume(GeneratorPrototype_throw, this, e);
      case "newborn":
        this[generatorStateSym] = "closed";
      case "closed":
        throw e;
      case "executing":
      default:
        throw TypeError();
    }
  },
  get [toStringTagSym]() {
    return "Generator";
  }
}), {
  [iteratorSym]: {enumerable: false},
  [mozIteratorSym]: {enumerable: false},
  next: {enumerable: false},
  send: {enumerable: false},
  close: {enumerable: false},
  throw: {enumerable: false},
  [toStringTagSym]: {enumerable: false},
});

})();
