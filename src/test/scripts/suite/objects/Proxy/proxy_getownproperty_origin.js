/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals
} = Assert;

// Test case 1 [Object.getOwnPropertyDescriptor]
{
  let specialDesc = {value: 123, writable: true, configurable: true, special: "x"};
  let desc = Object.getOwnPropertyDescriptor(new Proxy({}, {getOwnPropertyDescriptor: () => specialDesc}));
  let normalizedDesc = {value: 123, writable: true, enumerable: false, configurable: true};

  assertEquals(normalizedDesc, desc);
}

// Test case 2 [Reflect.getOwnPropertyDescriptor]
{
  let specialDesc = {value: 123, writable: true, configurable: true, special: "x"};
  let desc = Reflect.getOwnPropertyDescriptor(new Proxy({}, {getOwnPropertyDescriptor: () => specialDesc}));
  let normalizedDesc = {value: 123, writable: true, enumerable: false, configurable: true};

  assertEquals(normalizedDesc, desc);
}
