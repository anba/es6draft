/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals
} = Assert;

// Assert legacy arguments properties are returned correctly

// [[OwnPropertyKeys]] for legacy arguments, no parameters
{
  function f() { return f.arguments }
  assertEquals(["length", "callee"], Object.getOwnPropertyNames(f()));
}

// [[OwnPropertyKeys]] for legacy arguments, parameters
{
  function f(a) { return f.arguments }
  assertEquals(["0", "length", "callee"], Object.getOwnPropertyNames(f(1)));
}

// [[OwnPropertyKeys]] for legacy arguments, too few parameters
{
  function f(a) { return f.arguments }
  assertEquals(["length", "callee"], Object.getOwnPropertyNames(f()));
}

// [[OwnPropertyKeys]] for legacy arguments, too many parameters
{
  function f() { return f.arguments }
  assertEquals(["0", "length", "callee"], Object.getOwnPropertyNames(f(1)));
}
