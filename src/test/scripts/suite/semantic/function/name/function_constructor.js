/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

const GeneratorFunction = Object.getPrototypeOf(function*(){}).constructor;

// Function/GeneratorFunction constructor
{
  let f = Function("");
  assertFunctionName(f, "anonymous");

  let g = GeneratorFunction("");
  assertFunctionName(g, "anonymous");
}

// Delayed initialisation for Function/GeneratorFunction constructor
{
  let f = new class extends Function { constructor(){ /* no super */ } };
  Object.defineProperty(f, "name", {value: "own-name", writable: false, enumerable: false, configurable: true});
  Function.call(f, "");
  assertFunctionName(f, "own-name");

  let g = new class extends GeneratorFunction { constructor(){ /* no super */ } };
  Object.defineProperty(g, "name", {value: "own-name", writable: false, enumerable: false, configurable: true});
  GeneratorFunction.call(g, "");
  assertFunctionName(g, "own-name");
}
