/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// Assert function properties are returned in property creation order for built-in functions.

// [[OwnPropertyKeys]] for built-in function, no other properties present.
{
  let f = Array.prototype.unshift;
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length", "name"], keys);
}

// Property added before reifying default properties.
{
  let f = Array.prototype.shift;
  Object.defineProperty(f, "foobar", {});
  Object.defineProperty(f, "length", {get(){}});
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length", "name", "foobar"], keys);
}

// Default property removed and other default reified.
{
  let f = Array.prototype.slice;
  delete f.name;
  Object.defineProperty(f, "length", {get(){}});
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length"], keys);
}

// Default property removed and other default reified, with additional property.
{
  let f = Array.prototype.splice;
  delete f.name;
  Object.defineProperty(f, "foobar", {});
  Object.defineProperty(f, "length", {get(){}});
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length", "foobar"], keys);
}
