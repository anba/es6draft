/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

function assertAnonymousFunction(f) {
  return assertFalse(f.hasOwnProperty("name"));
}

// IsIdentifierRef() returns only true for IdentifierReference, but not for parenthesized IdentifierReference
// TODO: Possibly a spec bug?
{
  let f1, f2, f3, f4;
  f1 = function() {};
  f2 = (function() {});
  (f3) = function() {};
  (f4) = (function() {});

  assertFunctionName(f1, "f1");
  assertFunctionName(f2, "f2");
  assertAnonymousFunction(f3);
  assertAnonymousFunction(f4);
}
