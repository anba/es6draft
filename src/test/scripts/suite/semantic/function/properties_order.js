/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals
} = Assert;

// Assert function properties are returned in property creation order


// [[OwnPropertyKeys]] for non-strict, anonymous function
{
  let f = (0, function(){});
  let keys = Object.getOwnPropertyNames(f);

  // ignore legacy properties if present
  keys = keys.filter(n => n !== "caller" && n !== "arguments");

  assertEquals(["length", "prototype"], keys);
}

// [[OwnPropertyKeys]] for non-strict, implicitly named function
{
  let f = function(){};
  let keys = Object.getOwnPropertyNames(f);

  // ignore legacy properties if present
  keys = keys.filter(n => n !== "caller" && n !== "arguments");

  assertEquals(["length", "prototype", "name"], keys);
}

// [[OwnPropertyKeys]] for non-strict, explicitly named function
{
  let f = function F(){};
  let keys = Object.getOwnPropertyNames(f);

  // ignore legacy properties if present
  keys = keys.filter(n => n !== "caller" && n !== "arguments");

  assertEquals(["length", "prototype", "name"], keys);
}

// [[OwnPropertyKeys]] for strict function, anonymous function
{
  let f = (0, function(){"use strict"});
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length", "prototype"], keys);
}

// [[OwnPropertyKeys]] for strict function, implicitly named function
{
  let f = function(){"use strict"};
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length", "prototype", "name"], keys);
}

// [[OwnPropertyKeys]] for strict function, explicitly named function
{
  let f = function F(){"use strict"};
  let keys = Object.getOwnPropertyNames(f);

  assertEquals(["length", "prototype", "name"], keys);
}
