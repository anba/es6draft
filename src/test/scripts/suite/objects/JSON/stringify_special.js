/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertThrows,
  assertTrue,
  assertFalse,
  assertEquals,
  assertDataProperty,
  assertAccessorProperty,
  fail,
} = Assert;

// Tested implementations: JSC, Nashorn, V8, SpiderMonkey


// V8, JSC
{
  let replacerCalled = false;
  let toStringCalled = false;
  let replacer = Object.defineProperty([], 0, {
    get() {
      assertFalse(replacerCalled);
      assertFalse(toStringCalled);
      replacerCalled = true;
      return "a";
    }
  });
  let gap = new String();
  gap.toString = () => {
    assertFalse(toStringCalled);
    toStringCalled = true;
    return "";
  };
  JSON.stringify({a:0, b:1}, replacer, gap);
  assertTrue(replacerCalled);
  assertTrue(toStringCalled);
}

// V8, JSC
{
  let replacerCalled = false;
  let valueOfCalled = false;
  let replacer = Object.defineProperty([], 0, {
    get() {
      assertFalse(replacerCalled);
      assertFalse(valueOfCalled);
      replacerCalled = true;
      return "a";
    }
  });
  let gap = new Number();
  gap.valueOf = () => {
    assertFalse(valueOfCalled);
    valueOfCalled = true;
    return 0;
  };
  JSON.stringify({a:0, b:1}, replacer, gap);
  assertTrue(replacerCalled);
  assertTrue(valueOfCalled);
}

// V8
{
  let toStringCalled = false, valueOfCalled = false;
  let r = new Number();
  r.toString = () => {
    toStringCalled = true;
    return 0;
  };
  r.valueOf = () => {
    valueOfCalled = true;
    return 0;
  };
  JSON.stringify([], [r]);
  assertTrue(toStringCalled);
  assertFalse(valueOfCalled);
}

// V8, JSC
{
  let getterACalled = false, getterBCalled = false, getterProtoBCalled = false;
  JSON.stringify({
    get a() {
      getterACalled = true;
      delete this.b;
    },
    get b() {
      getterBCalled = true;
    },
    __proto__: {
      get b() {
        getterProtoBCalled = true;
      }
    }
  }, {/* ignore */});
  assertTrue(getterACalled);
  assertFalse(getterBCalled);
  assertTrue(getterProtoBCalled);
}

// JSC
{
  let valueOfCalled = false, toStringCalled = false;
  let gap = new Boolean();
  gap.valueOf = () => {
    valueOfCalled = true;
    return {};
  };
  gap.toString = () => {
    toStringCalled = true;
  };
  JSON.stringify([], [], gap);
  assertFalse(valueOfCalled);
  assertFalse(toStringCalled);
}

// JSC
{
  let valueOfCalled = false, toStringCalled = false;
  let gap = new Boolean();
  gap.valueOf = () => {
    valueOfCalled = true;
  };
  gap.toString = () => {
    toStringCalled = true;
    return {};
  };
  JSON.stringify([], [], gap);
  assertFalse(valueOfCalled);
  assertFalse(toStringCalled);
}

// Nashorn
{
  let callCount = 0;
  JSON.stringify({get a(){ callCount++ }}, ["a", "a"]);
  assertSame(1, callCount);
}

// Nashorn
{
  let setterCalled = false;
  try {
    Object.defineProperty(Object.prototype, "", {
      set() { setterCalled = true },
      configurable: true
    });
    JSON.stringify([1, 2]);
  } finally {
    let success = delete Object.prototype[""];
    assertTrue(success);
  }
  assertFalse(setterCalled);
}

// JSC, Nashorn
{
  let type = "";
  JSON.stringify([{}], function replacer(k, v) {
    if (k != "") {
      type = typeof k;
    }
    return v;
  });
  assertSame("string", type);
}

// JSC
{
  let s;
  try {
    Array.prototype[0] = 123;
    s = JSON.stringify([,]);
  } finally {
    let success = delete Array.prototype[0];
    assertTrue(success);
  }
  assertSame("[123]", s);
}
