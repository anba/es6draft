/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Generator(global) {
"use strict";

const Object = global.Object,
      Function = global.Function;

const Object_defineProperty = Object.defineProperty,
      Object_hasOwnProperty = Function.prototype.call.bind(Object.prototype.hasOwnProperty);

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const GeneratorPrototype = Object.getPrototypeOf(function*(){}).prototype;
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
