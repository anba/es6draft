/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertEquals
} = Assert;

// Throws TypeError if ownKeys result contains elements other than String and Symbol
assertThrows(TypeError, () => Reflect.ownKeys(new Proxy({}, {ownKeys: () => [0]})));

// Throws TypeError if ownKeys is primitive
assertThrows(TypeError, () => Reflect.ownKeys(new Proxy({}, {ownKeys: () => ""})));

// Non-Array result is allowed
assertEquals([], Reflect.ownKeys(new Proxy({}, {ownKeys: () => ({length: 0})})));
assertEquals(["a"], Reflect.ownKeys(new Proxy({}, {ownKeys: () => ({length: 1, 0: "a"})})));

// Duplicate property keys and non-extensible target, duplicate key is configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: true},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  Reflect.preventExtensions(o);
  assertThrows(TypeError, () => Reflect.ownKeys(p));
}

// Duplicate property keys and non-extensible target, duplicate key is non-configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: false},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  Reflect.preventExtensions(o);
  assertThrows(TypeError, () => Reflect.ownKeys(p));
}

// Duplicate property keys not present in outer proxy, duplicate key is configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: true},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  let p2 = new Proxy(p, {
    ownKeys() {
      return ["a", "b"];
    }
  });
  assertThrows(TypeError, () => Reflect.ownKeys(p2));
}

// Duplicate property keys present in outer proxy, duplicate key is configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: true},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  let p2 = new Proxy(p, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  assertThrows(TypeError, () => Reflect.ownKeys(p2));
}

// Duplicate property keys present multiple times in outer proxy, duplicate key is configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: true},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  let p2 = new Proxy(p, {
    ownKeys() {
      return ["a", "b", "b", "b"];
    }
  });
  assertThrows(TypeError, () => Reflect.ownKeys(p2));
}

// Duplicate property keys not present in outer proxy, duplicate key is non-configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: false},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  let p2 = new Proxy(p, {
    ownKeys() {
      return ["a", "b"];
    }
  });
  assertThrows(TypeError, () => Reflect.ownKeys(p2));
}

// Duplicate property keys present in outer proxy, duplicate key is non-configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: false},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  let p2 = new Proxy(p, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  assertThrows(TypeError, () => Reflect.ownKeys(p2));
}

// Duplicate property keys present multiple times in outer proxy, duplicate key is non-configurable
{
  let o = Object.defineProperties({}, {
    a: {value: 0, configurable: false},
    b: {value: 0, configurable: false},
  });
  let p = new Proxy(o, {
    ownKeys() {
      return ["a", "b", "b"];
    }
  });
  let p2 = new Proxy(p, {
    ownKeys() {
      return ["a", "b", "b", "b"];
    }
  });
  assertThrows(TypeError, () => Reflect.ownKeys(p2));
}
