/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Test case 1 [Object.getOwnPropertyDescriptor]
{
  let specialDesc = {value: 123, writable: true, configurable: true, special: "x"};
  let desc = Object.getOwnPropertyDescriptor(new Proxy({}, {getOwnPropertyDescriptor: () => specialDesc}));

  assertSame(specialDesc, desc);
}

// Test case 2 [Reflect.getOwnPropertyDescriptor]
{
  let specialDesc = {value: 123, writable: true, configurable: true, special: "x"};
  let desc = Reflect.getOwnPropertyDescriptor(new Proxy({}, {getOwnPropertyDescriptor: () => specialDesc}));

  assertSame(specialDesc, desc);
}

// Test case 3 [Object.mixin]
{
  let specialDesc = {value: 123, writable: true, configurable: true, special: "x"};
  let source = new Proxy({}, {
    getOwnPropertyDescriptor: () => specialDesc,
    ownKeys: () => ["prop"].values(),
  });
  let defines = [];
  let target = new Proxy({}, {
    defineProperty: (t, propertyKey, descriptor) => {
      defines.push({propertyKey, descriptor});
      return true;
    }
  });
  Object.mixin(target, source);
  assertSame(1, defines.length);
  assertSame("prop", defines[0].propertyKey);
  assertSame(specialDesc, defines[0].descriptor);
}
