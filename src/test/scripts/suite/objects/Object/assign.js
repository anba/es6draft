/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertThrows,
  assertInstanceOf,
  assertSame,
  assertFalse,
  assertTrue,
  assertDataProperty,
  fail,
} = Assert;


/* 19.1.3.1  Object.assign ( target, source ) */

assertBuiltinFunction(Object.assign, "assign", 2);

// Calls ToObject() for target and source
{
  assertThrows(() => Object.assign(null, null), TypeError);
  assertThrows(() => Object.assign(null, {}), TypeError);
  assertThrows(() => Object.assign({}, null), TypeError);
  assertInstanceOf(Boolean, Object.assign(true, {}));
  assertInstanceOf(Number, Object.assign(1, {}));
  assertInstanceOf(String, Object.assign("string", {}));
  assertInstanceOf(Symbol, Object.assign(Symbol.create, {}));
  let o = {};
  assertSame(o, Object.assign(o, {}));
}

// Invokes [[OwnPropertyKeys]] on ToObject(source)
{
  assertThrows(() => Object.assign(null, new Proxy({}, {
    ownKeys: () => fail `ToObject(target) succeeded`
  })), TypeError);

  let ownKeysCalled = false;
  Object.assign({}, new Proxy({}, {
    ownKeys() {
      ownKeysCalled = true;
      return [].values();
    }
  }));
  assertTrue(ownKeysCalled);
}

// Ensure correct property traversal
{
  let log = "";
  let source = new Proxy({a: 1, b: 2}, {
    ownKeys: () => ["b", "c", "a"].values(),
    getOwnPropertyDescriptor(t, pk) {
      log += "#" + pk;
      return Reflect.getOwnPropertyDescriptor(t, pk);
    },
    get(t, pk, r) {
      log += "-" + pk;
      return Reflect.get(t, pk, r);
    },
  });
  Object.assign({}, source);
  assertSame("#b-b#c#a-a", log);
}

// Only [[Enumerable]] properties are assigned to target
{
  let source = Object.defineProperties({}, {
    a: {value: 1, enumerable: true},
    b: {value: 2, enumerable: false},
  });
  let target = Object.assign({}, source);
  assertTrue("a" in target);
  assertFalse("b" in target);
}

// Enumerability is decided on-time, not before main loop (1)
{
  let getterCalled = false;
  let source = new Proxy({
    get a() { getterCalled = true },
    get b() { Object.defineProperty(this, "a", {enumerable: false}) },
  }, {
    ownKeys: () => ["b", "a"].values()
  });
  Object.assign({}, source);
  assertFalse(getterCalled);
}

// Enumerability is decided on-time, not before main loop (2)
{
  let getterCalled = false;
  let source = new Proxy({
    get a() { getterCalled = true },
    get b() { Object.defineProperty(this, "a", {enumerable: true}) },
  }, {
    ownKeys: () => ["b", "a"].values()
  });
  Object.defineProperty(source, "a", {enumerable: false});
  Object.assign({}, source);
  assertTrue(getterCalled);
}

// Properties are retrieved through Get()
{
  let getterCalled = false;
  Object.assign({}, {get a() { getterCalled = true }});
  assertTrue(getterCalled);
}

// Properties are assigned through Put()
{
  let setterCalled = false;
  Object.assign({set a(v) { setterCalled = v }}, {a: true});
  assertTrue(setterCalled);
}

// Properties are assigned through Put(): Existing property attributes are not altered
{
  let source = {a: 1, b: 2, c: 3};
  let target = {a: 0, b: 0, c: 0};
  Object.defineProperty(target, "a", {enumerable: false});
  Object.defineProperty(target, "b", {configurable: false});
  Object.defineProperty(target, "c", {enumerable: false, configurable: false});
  Object.assign(target, source);
  assertDataProperty(target, "a", {value: 1, writable: true, enumerable: false, configurable: true});
  assertDataProperty(target, "b", {value: 2, writable: true, enumerable: true, configurable: false});
  assertDataProperty(target, "c", {value: 3, writable: true, enumerable: false, configurable: false});
}

// Properties are assigned through Put(): Throws TypeError if non-writable
{
  let source = {a: 1};
  let target = {a: 0};
  Object.defineProperty(target, "a", {writable: false});
  assertThrows(() => Object.assign(target, source), TypeError);
  assertDataProperty(target, "a", {value: 0, writable: false, enumerable: true, configurable: true});
}

// Put() creates standard properties; Property attributes from source are ignored
{
  let source = {a: 1, b: 2, c: 3, get d() { return 4 }};
  Object.defineProperty(source, "b", {writable: false});
  Object.defineProperty(source, "c", {configurable: false});
  let target = Object.assign({}, source);
  assertDataProperty(target, "a", {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(target, "b", {value: 2, writable: true, enumerable: true, configurable: true});
  assertDataProperty(target, "c", {value: 3, writable: true, enumerable: true, configurable: true});
  assertDataProperty(target, "d", {value: 4, writable: true, enumerable: true, configurable: true});
}

// Properties created during traversal are not copied
{
  let source = {get a() { this.b = 2 }};
  let target = Object.assign({}, source);
  assertTrue("a" in target);
  assertFalse("b" in target);
}

// Properties created during traversal are copied - Proxy edition
{
  let hasB = false;
  let source = new Proxy({
    get a() { hasB = "b" in this },
  }, {
    ownKeys: () => function*() {
      yield "a";
      source.b = 2;
      yield "b";
    }()
  });
  let target = Object.assign({}, source);
  assertTrue("a" in target);
  assertTrue("b" in target);
  assertFalse(hasB);
}

// Properties deleted during traversal are not copied
{
  let source = new Proxy({
    get a() { delete this.b },
    b: 2,
  }, {
    ownKeys: () => ["a", "b"].values()
  });
  let target = Object.assign({}, source);
  assertTrue("a" in target);
  assertFalse("b" in target);
}

// Properties first deleted and then recreated during traversal are copied (1)
{
  let source = new Proxy({
    get a() { delete this.c },
    get b() { this.c = 4 },
    c: 3,
  }, {
    ownKeys: () => ["a", "b", "c"].values()
  });
  let target = Object.assign({}, source);
  assertTrue("a" in target);
  assertTrue("b" in target);
  assertTrue("c" in target);
  assertDataProperty(target, "c", {value: 4, writable: true, enumerable: true, configurable: true});
}

// Properties first deleted and then recreated during traversal are copied (2)
{
  let source = new Proxy({
    get a() { delete this.c },
    get b() { this.c = 4 },
    c: 3,
  }, {
    ownKeys: () => ["a", "c", "b"].values()
  });
  let target = Object.assign({}, source);
  assertTrue("a" in target);
  assertTrue("b" in target);
  assertFalse("c" in target);
}

// String and Symbol valued properties are copied
{
  let keyA = "str-prop", keyB = Symbol("sym-prop");
  let source = {[keyA]: 1, [keyB]: 2};
  let target = Object.assign({}, source);
  assertDataProperty(target, keyA, {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(target, keyB, {value: 2, writable: true, enumerable: true, configurable: true});
}

// Intermediate exceptions do not stop property traversal, first exception is reported (1)
{
  class ErrorA extends Error {}
  class ErrorB extends Error {}
  let log = "";
  let source = new Proxy({}, {
    getOwnPropertyDescriptor(t, pk) {
      log += pk;
      throw new (pk === "a" ? ErrorA : ErrorB);
    },
    ownKeys: () => ["b", "a"].values()
  });
  assertThrows(() => Object.assign({}, source), ErrorB);
  assertSame("ba", log);
}

// Intermediate exceptions do not stop property traversal, first exception is reported (2)
{
  class ErrorA extends Error {}
  class ErrorB extends Error {}
  let log = "";
  let source = new Proxy({
    get a() { log += "a"; throw new ErrorA },
    get b() { log += "b"; throw new ErrorB },
  }, {
    ownKeys: () => ["b", "a"].values()
  });
  assertThrows(() => Object.assign({}, source), ErrorB);
  assertSame("ba", log);
}

// Intermediate exceptions do not stop property traversal, first exception is reported (3)
{
  class ErrorA extends Error {}
  class ErrorB extends Error {}
  let log = "";
  let source = new Proxy({a: 1, b: 2}, {
    ownKeys: () => ["b", "a"].values()
  });
  let target = {
    set a(v) { log += "a"; throw new ErrorA },
    set b(v) { log += "b"; throw new ErrorB },
  };
  assertThrows(() => Object.assign(target, source), ErrorB);
  assertSame("ba", log);
}

// Intermediate exceptions do not stop property traversal, first exception is reported (4)
{
  class ErrorGetOwnProperty extends Error {}
  class ErrorGet extends Error {}
  class ErrorSet extends Error {}
  let source = new Proxy({
    get a() { throw new ErrorGet }
  }, {
    getOwnPropertyDescriptor(t, pk) {
      throw new ErrorGetOwnProperty;
    }
  });
  let target = {
    set a(v) { throw new ErrorSet }
  };
  assertThrows(() => Object.assign({}, source), ErrorGetOwnProperty);
}

// Exceptions in Iterator directly stop property traversal (1)
{
  class ErrorNext extends Error {}
  let getterCalled = false;
  let source = new Proxy({
    get a() { getterCalled = true }
  }, {
    ownKeys: () => ({next() { throw new ErrorNext }})
  });
  assertThrows(() => Object.assign({}, source), ErrorNext);
  assertFalse(getterCalled);
}

// Exceptions in Iterator directly stop property traversal (2)
{
  class ErrorDone extends Error {}
  class ErrorValue extends Error {}
  let getterCalled = false;
  let source = new Proxy({
    get a() { getterCalled = true }
  }, {
    ownKeys: () => ({
      next() {
        return {
          get done() { throw new ErrorDone },
          get value() { throw new ErrorValue },
        };
      }
    })
  });
  assertThrows(() => Object.assign({}, source), ErrorDone);
  assertFalse(getterCalled);
}

// Exceptions in Iterator directly stop property traversal (3)
{
  class ErrorDone extends Error {}
  class ErrorValue extends Error {}
  let getterCalled = false;
  let source = new Proxy({
    get a() { getterCalled = true }
  }, {
    ownKeys: () => ({
      next() {
        return {
          get done() { return false },
          get value() { throw new ErrorValue },
        };
      }
    })
  });
  assertThrows(() => Object.assign({}, source), ErrorValue);
  assertFalse(getterCalled);
}
