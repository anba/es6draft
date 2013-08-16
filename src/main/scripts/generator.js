/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Generator(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      TypeError = global.TypeError;

const Object_defineProperty = Object.defineProperty,
      Object_getPrototypeOf = Object.getPrototypeOf,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty);

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const Generator = Object.getPrototypeOf(function*(){});
const hasInstanceSym = global.getSym("@@hasInstance");

Object.defineProperty(Generator, hasInstanceSym, {
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

const GeneratorPrototype = Generator.prototype;
const GeneratorPrototype_next = GeneratorPrototype.next;
const genState = global.newSym("genState");

Object.defineProperty(GeneratorPrototype, "next", {
  value(...args) {
    if (!Object_hasOwnProperty(this, genState)) {
      Object_defineProperty(this, genState, {__proto__: null, value: true});
      // drop arguments on first call to next()
      return $CallFunction(GeneratorPrototype_next, this);
    }
    return $CallFunction(GeneratorPrototype_next, this, ...args);
  },
  writable: true, enumerable: false, configurable: true
});

const iteratorSym = global.getSym("@@iterator");

Object.defineProperty(Object.prototype, iteratorSym, {
  get() { return () => ({__proto__: null, next: () => Object(this.next())}) },
  enumerable: false, configurable: true
});

})(this);
