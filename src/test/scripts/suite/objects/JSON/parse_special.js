/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertTrue,
  assertFalse,
  assertEquals,
  assertDataProperty,
  assertAccessorProperty,
  fail,
} = Assert;

// Tested implementations: JSC, Nashorn, V8, SpiderMonkey


// Nashorn, V8, JSC
{
  let names = ["a", "b", ""];
  let set = () => fail `Unexpected [[Put]]`;
  let get = () => 2;
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      Object.defineProperty(this, "b", { set, get });
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: true, enumerable: true, configurable: true});
}

// Nashorn, V8, JSC
{
  let names = ["a", "b", ""];
  let set = () => fail `Unexpected [[Put]]`;
  let get = () => 2;
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      Object.defineProperty(this, "b", { set, get, configurable: true });
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: true, enumerable: true, configurable: true});
}

// SpiderMonkey, Nashorn, V8, JSC
{
  let names = ["a", "b", ""];
  let set = () => fail `Unexpected [[Put]]`;
  let get = () => 2;
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      Object.defineProperty(this, "b", { set, get, configurable: false });
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertAccessorProperty(o, "b", {get, set, enumerable: true, configurable: false});
}

// SpiderMonkey, Nashorn, V8, JSC
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      Object.seal(this);
    }
    if (name === "b") {
      return 2; // new value must be ignored!
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertFalse(Object.isExtensible(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: false});
  assertDataProperty(o, "b", {value: 1, writable: true, enumerable: true, configurable: false});
}

// SpiderMonkey, V8, Nashorn
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      delete this.b;
      Object.seal(this);
    }
    if (name === "b") {
      return 2; // new value must be ignored!
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a"], Object.getOwnPropertyNames(o));
  assertFalse(Object.isExtensible(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: false});
  assertFalse(o.hasOwnProperty("b"));
}

// SpiderMonkey
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      Object.freeze(this);
    }
    if (name === "b") {
      return 2; // new value must be ignored!
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertFalse(Object.isExtensible(o));
  assertDataProperty(o, "a", {value: 0, writable: false, enumerable: true, configurable: false});
  assertDataProperty(o, "b", {value: 1, writable: false, enumerable: true, configurable: false});
}

// SpiderMonkey, V8, Nashorn
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      delete this.b;
      Object.freeze(this);
    }
    if (name === "b") {
      return 2; // new value must be ignored!
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a"], Object.getOwnPropertyNames(o));
  assertFalse(Object.isExtensible(o));
  assertDataProperty(o, "a", {value: 0, writable: false, enumerable: true, configurable: false});
  assertFalse(o.hasOwnProperty("b"));
}

// Nashorn
{
  let array = Object.defineProperty([100], "0", { enumerable: false });
  let names = ["a", "0", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      this.b = array;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: array, writable: true, enumerable: true, configurable: true});
}

// JSC
{
  let names = ["0", "1", "2", ""];
  let o = JSON.parse('[0, 1, 2]', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "0") {
      Array.prototype[1] = 33;
      delete this[1];
    }
    if (name === "2") {
      delete Array.prototype[1];
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["0", "1", "2", "length"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "0", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "1", {value: 33, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "2", {value: 2, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "length", {value: 3, writable: true, enumerable: false, configurable: false});
}

// V8
{
  let names = ["0", "1", "2", ""];
  let o = JSON.parse('[0, 1, 2]', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "0") {
      Object.defineProperty(this, "1", { configurable: false });
    }
    if (name === "1") {
      return;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["0", "1", "2", "length"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "0", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "1", {value: 1, writable: true, enumerable: true, configurable: false});
  assertDataProperty(o, "2", {value: 2, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "length", {value: 3, writable: true, enumerable: false, configurable: false});
}

// JSC
{
  let names = ["0", ""];
  let o = JSON.parse('[0]', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "0") {
      this.push(1);
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["0", "1", "length"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "0", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "1", {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "length", {value: 2, writable: true, enumerable: false, configurable: false});
}

// Nashorn, JSC, V8, SpiderMonkey
{
  let names = ["0", "1", ""];
  let o = JSON.parse('[0, 1]', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "0") {
      Object.defineProperty(this, 1, {configurable: false});
    }
    if (name === "1") {
      return 33;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["0", "1", "length"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "0", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "1", {value: 1, writable: true, enumerable: true, configurable: false});
  assertDataProperty(o, "length", {value: 2, writable: true, enumerable: false, configurable: false});
}

// JSC, V8
{
  let names = ["0", ""];
  let o = JSON.parse('[0]', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "0") {
      this.length = 0;
    }
    if (name === "0") {
      return;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["length"], Object.getOwnPropertyNames(o));
  assertFalse(o.hasOwnProperty(0));
  assertDataProperty(o, "length", {value: 0, writable: true, enumerable: false, configurable: false});
}

// Nashorn
{
  let names = ["0", "1", ""];
  let o = JSON.parse('[0, 1]', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "1") {
      this.length = 0;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["1", "length"], Object.getOwnPropertyNames(o));
  assertFalse(o.hasOwnProperty(0));
  assertDataProperty(o, "1", {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "length", {value: 2, writable: true, enumerable: false, configurable: false});
}

// V8
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      delete this.b;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertFalse(o.hasOwnProperty("b"));
}

// V8
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      delete this.b;
    }
    if (name === "b") {
      return 2;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: true, enumerable: true, configurable: true});
}

// JSC
{
  let names = ["a", "b", ""];
  let o = JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
    assertSame(names.shift(), name);
    if (name === "a") {
      delete this.b;
      this.__proto__ = {
        set b(_) {
          fail `Unexpected [[Put]]`;
        }
      };
    }
    if (name === "b") {
      return 2;
    }
    return value;
  });
  assertSame(0, names.length, `Unvisited names: ${names}`);
  assertEquals(["a", "b"], Object.getOwnPropertyNames(o));
  assertDataProperty(o, "a", {value: 0, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: true, enumerable: true, configurable: true});
}
