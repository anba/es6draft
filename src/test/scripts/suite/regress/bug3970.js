/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse, assertThrows, fail
} = Assert;

// 9.4.2.3, 19.1.3.6, 24.3.1.1, 24.3.2, 24.3.2.1: Missing ReturnIfAbrupt after IsArray
// https://bugs.ecmascript.org/show_bug.cgi?id=3970

// 9.4.2.3
{
  let lengthCalled = false;
  let {proxy, revoke} = Proxy.revocable([], new Proxy({
    get(t, pk, r) {
      if (pk === "length") {
        lengthCalled = true;
        revoke();
        return 0;
      }
      fail `unexpected get: ${String(pk)}`;
    }
  }, {
    get(t, pk, r) {
      if (pk === "get") {
        return Reflect.get(t, pk, r);
      }
      fail `unexpected trap: ${pk}`;
    }
  }));

  assertFalse(lengthCalled);
  assertThrows(TypeError, () => Array.prototype.map.call(proxy, () => {}));
  assertTrue(lengthCalled);
}

// 19.1.3.6
{
  let {proxy, revoke} = Proxy.revocable([], new Proxy({}, {
    get() {
      fail `trap called`;
    }
  }));
  revoke();
  assertThrows(TypeError, () => Object.prototype.toString.call(proxy));
}

// 24.3.1.1
{
  function reviver(name, value) {
    if (name === "a") {
      let {proxy, revoke} = Proxy.revocable([], new Proxy({}, {
        get() {
          fail `trap called`;
        }
      }));
      revoke();
      this.b = proxy;
    }
    return value;
  }
  assertThrows(TypeError, () => JSON.parse('{"a": 0, "b": 1}', reviver));
}

// 24.3.2
{
  let {proxy, revoke} = Proxy.revocable([], new Proxy({}, {
    get() {
      fail `trap called`;
    }
  }));
  revoke();
  assertThrows(TypeError, () => JSON.stringify({}, proxy));
}

// 24.3.2.1
{
  function replacer(key, value) {
    if (key === "a") {
      let {proxy, revoke} = Proxy.revocable([], new Proxy({}, {
        get() {
          fail `trap called`;
        }
      }));
      revoke();
      return proxy;
    }
    return value;
  }
  assertThrows(TypeError, () => JSON.stringify({a: 0}, replacer));

  assertThrows(TypeError, () => JSON.stringify({
    toJSON() {
      let {proxy, revoke} = Proxy.revocable([], new Proxy({}, {
        get() {
          fail `trap called`;
        }
      }));
      revoke();
      return proxy;
    }
  }));
}
