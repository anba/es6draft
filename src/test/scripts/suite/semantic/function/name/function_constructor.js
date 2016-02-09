/*
 * Copyright (c) 2012-2015 André Bargull
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
