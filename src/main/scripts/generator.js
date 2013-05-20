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

const GeneratorPrototype = Object.getPrototypeOf(function*(){}).prototype;
const GeneratorPrototype_next = GeneratorPrototype.next;
const genState = newSym("genState");

Object.defineProperty(GeneratorPrototype, "next", {
  value() {
    if (!Object_hasOwnProperty(this, genState)) {
      Object_defineProperty(this, genState, {value: true});
    }
    return GeneratorPrototype_next.apply(this, arguments);
  },
  writable: true, enumerable: false, configurable: true
});

// add send() function to prototype
Object.defineProperty(GeneratorPrototype, "send", {
  value() {
    if (!Object_hasOwnProperty(this, genState)) {
      Object_defineProperty(this, genState, {value: true});
      // drop arguments on first call to next()
      return GeneratorPrototype_next.call(this);
    }
    return GeneratorPrototype_next.apply(this, arguments);
  },
  writable: true, enumerable: false, configurable: true
});

})(this);
