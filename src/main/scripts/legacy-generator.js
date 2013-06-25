/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function LegacyGenerator(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      StopIteration = global.StopIteration;

const Object_defineProperty = Object.defineProperty,
      Object_getPrototypeOf = Object.getPrototypeOf,
      Object_toString = Function.prototype.call.bind(Object.prototype.toString),
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty);

const GeneratorFunction = Object.getPrototypeOf(function*(){}).constructor;
const GeneratorPrototype = Object.getPrototypeOf(function*(){}).prototype;
const GeneratorPrototype_next = GeneratorPrototype.next,
      GeneratorPrototype_throw = GeneratorPrototype.throw;

const iteratorSym = getSym("@@iterator");

//  “newborn”, “executing”, “suspended”, or “closed”
const generatorStateSym = newSym("generatorState");

function GeneratorState(g) {
  // no proper way to test for generators in scripts 
  if (Object_toString(g) == "[object Generator]") {
    if (Object_getPrototypeOf(g) === GeneratorPrototype) {
      return "closed";
    }
    if (!Object_hasOwnProperty(g, generatorStateSym)) {
      Object_defineProperty(g, generatorStateSym, {value: "newborn", writable: true});
    }
    return g[generatorStateSym];
  }
}

function GeneratorResume(fn, g, v) {
  g[generatorStateSym] = "executing";

  try {
    var result = fn.call(g, v);
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

Object.defineProperties(Object.assign(Function.prototype, {
  isGenerator() {
    return this instanceof GeneratorFunction;
  },
}), {
  isGenerator: {enumerable: false},
});

Object.defineProperty(GeneratorPrototype, iteratorSym, {
  value() {
    return {
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
    }
  }
});

Object.defineProperties(Object.assign(GeneratorPrototype, {
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
}), {
  next: {enumerable: false},
  send: {enumerable: false},
  close: {enumerable: false},
  throw: {enumerable: false},
});

})(this);
