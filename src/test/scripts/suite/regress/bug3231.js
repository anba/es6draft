/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, fail
} = Assert;

// 7.3.11, 7.3.12, 19.1.2.1, 19.1.2.3.1: Different placeholder for pendingException
// https://bugs.ecmascript.org/show_bug.cgi?id=3231

function makeLoggingProxy(trap) {
  let logstring = "";
  let proxy = new Proxy({a: 0, b: 1}, {
    [trap](target, propertyKey) {
      logstring += propertyKey;
      if (propertyKey === "a") {
        throw void 0;
      }
      if (propertyKey === "b") {
        throw new Error();
      }
      fail `Unexpected property-key: ${propertyKey}`;
    },
    ownKeys() {
      return ["a", "b"];
    }
  });
  return {proxy, log: () => logstring};
}

// SetIntegrityLevel (Object.seal)
{
  let {proxy, log} = makeLoggingProxy("defineProperty");
  try {
    Object.seal(proxy);
    fail `No exception thrown`;
  } catch (e) {
    assertUndefined(e);
  }
  assertSame("a", log());
}

// SetIntegrityLevel (Object.freeze)
{
  let {proxy, log} = makeLoggingProxy("getOwnPropertyDescriptor");
  try {
    Object.freeze(proxy);
    fail `No exception thrown`;
  } catch (e) {
    assertUndefined(e);
  }
  assertSame("a", log());
}

// TestIntegrityLevel (Object.isSealed)
{
  let {proxy, log} = makeLoggingProxy("getOwnPropertyDescriptor");
  Object.preventExtensions(proxy);
  try {
    Object.isSealed(proxy);
    fail `No exception thrown`;
  } catch (e) {
    assertUndefined(e);
  }
  assertSame("a", log());
}

// TestIntegrityLevel (Object.isFrozen)
{
  let {proxy, log} = makeLoggingProxy("getOwnPropertyDescriptor");
  Object.preventExtensions(proxy);
  try {
    Object.isFrozen(proxy);
    fail `No exception thrown`;
  } catch (e) {
    assertUndefined(e);
  }
  assertSame("a", log());
}

// Object.assign
{
  let {proxy, log} = makeLoggingProxy("getOwnPropertyDescriptor");
  try {
    Object.assign({}, proxy);
    fail `No exception thrown`;
  } catch (e) {
    assertUndefined(e);
  }
  assertSame("a", log());
}

// Object.defineProperties
{
  let props = new Proxy({a: {}, b: {}}, {ownKeys: () => ["a", "b"]});
  let {proxy, log} = makeLoggingProxy("defineProperty");
  try {
    Object.defineProperties(proxy, props);
    fail `No exception thrown`;
  } catch (e) {
    assertUndefined(e);
  }
  assertSame("a", log());
}
