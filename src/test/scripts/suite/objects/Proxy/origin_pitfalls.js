/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertEquals,
  assertThrows,
} = Assert;

// Proxy reports a configurable property as non-configurable
{
  let propertyName = "propertyName";
  let target = {[propertyName]: 0};

  // First assert the TypeError is emitted in the standard case
  let p1 = new Proxy(target, {
    getOwnPropertyDescriptor(t, pk) {
      return {value: 1, writable: true, enumerable: true, configurable: false};
    }
  });
  assertThrows(() => Object.getOwnPropertyDescriptor(p1, propertyName), TypeError);

  // Now test how [[Origin]] can fool callers of Object.getOwnPropertyDescriptor(). Idea:
  // - report the property as configurable when testing the object integrity rules in Proxy
  // - but then change the property descriptor object to report the property as non-configurable
  let p2 = new Proxy(target, {
    getOwnPropertyDescriptor(t, pk) {
      return {
        value: 1, writable: true, enumerable: true,
        get configurable() {
          delete this.configurable;
          this.configurable = false;
          return true;
        }
      };
    }
  });
  let desc = Object.getOwnPropertyDescriptor(p2, propertyName);
  assertSame(1, desc.value);
  assertSame(false, desc.configurable);
  assertSame(true, desc.writable);
  assertSame(true, desc.enumerable);
}

// Object.getOwnPropertyDescriptor() no longer returns either undefined or an object with Object.prototype as [[Prototype]]
{
  let p = new Proxy({}, {
    getOwnPropertyDescriptor() {
      // add configurable:true to pass object integrity tests
      return Object.assign(() => {}, {configurable: true});
    }
  });
  assertSame("function", typeof Object.getOwnPropertyDescriptor(p, "propertyName"));
}

// Object.getOwnPropertyDescriptor() returns an empty object
{
  let p = new Proxy({}, {
    getOwnPropertyDescriptor() {
      // temporarily add configurable:true to pass object integrity tests
      return {
        get configurable() {
          delete this.configurable;
          return true;
        }
      };
    }
  });
  assertEquals({}, Object.getOwnPropertyDescriptor(p, "propertyName"));
}
