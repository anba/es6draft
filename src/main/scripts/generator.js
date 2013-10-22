/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Generator(global) {
"use strict";

const {
  Object, Function, Symbol, TypeError,
} = global;

const Object_defineProperty = Object.defineProperty,
      Object_getPrototypeOf = Object.getPrototypeOf,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty);

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const Generator = Object.getPrototypeOf(function*(){});
const GeneratorPrototype = Generator.prototype;
const GeneratorPrototype_next = GeneratorPrototype.next;
const nextCalledOnce = Symbol("nextCalledOnce");

Object.defineProperty(Generator, Symbol.hasInstance, {
  value(O) {
    // OrdinaryHasInstance() without steps 1-2
    let C = this;
    if (Object(O) !== O) {
      return false;
    }
    let P = C.prototype;
    if (Object(P) !== P) {
      throw new TypeError();
    }
    for (;;) {
      O = Object_getPrototypeOf(O);
      if (O === null) {
        return false;
      }
      if (P === O) {
        return true;
      }
    }
  },
  writable: false, enumerable: false, configurable: true
});

Object.defineProperty(GeneratorPrototype, "next", {
  value(...args) {
    if (!Object_hasOwnProperty(this, nextCalledOnce)) {
      Object_defineProperty(this, nextCalledOnce, {__proto__: null, value: true});
      // drop arguments on first call to next()
      return $CallFunction(GeneratorPrototype_next, this);
    }
    return $CallFunction(GeneratorPrototype_next, this, ...args);
  },
  writable: true, enumerable: false, configurable: true
});

Object.defineProperty(Object.prototype, Symbol.iterator, {
  get() { return () => ({__proto__: null, next: () => Object(this.next())}) },
  enumerable: false, configurable: true
});

})(this);
