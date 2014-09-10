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
  assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(p1, propertyName));

  // Getters mutating property descriptor record have no effect
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
  let normalizedDesc = {value: 1, writable: true, enumerable: true, configurable: true};
  assertEquals(normalizedDesc, desc);
}

// Object.getOwnPropertyDescriptor() returns either undefined or an object with Object.prototype as [[Prototype]]
{
  let p = new Proxy({}, {
    getOwnPropertyDescriptor() {
      // add configurable:true to pass object integrity tests
      return Object.assign(() => {}, {configurable: true});
    }
  });
  let desc = Object.getOwnPropertyDescriptor(p, "propertyName");
  assertSame("object", typeof desc);
  assertSame(Object.prototype, Object.getPrototypeOf(desc));
}

// Object.getOwnPropertyDescriptor() returns a normalized property descriptor
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
  let normalizedDesc = {value: void 0, writable: false, enumerable: false, configurable: true};
  assertEquals(normalizedDesc, Object.getOwnPropertyDescriptor(p, "propertyName"));
}
