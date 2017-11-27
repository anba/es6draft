/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertNotNull, assertFalse, fail
} = Assert;

// 6.1.7.3 Invariants of the Essential Internal Methods: Prototype chain may have infinite length
// https://bugs.ecmascript.org/show_bug.cgi?id=2437

{
  let o = {}, p = new Proxy(o, {});
  Object.setPrototypeOf(o, p);
  assertSame(p, Object.getPrototypeOf(p));
}

{
  function infiniteProxy() {
    return new Proxy({}, {
      getPrototypeOf() {
        return new infiniteProxy();
      }
    });
  }
  let ws = new WeakSet();
  let p = infiniteProxy();

  let p0 = Object.getPrototypeOf(p);
  assertNotNull(p0);
  assertFalse(ws.has(p0));
  ws.add(p0);

  let p1 = Object.getPrototypeOf(p0);
  assertNotNull(p1);
  assertFalse(ws.has(p1));
  ws.add(p1);

  let p2 = Object.getPrototypeOf(p1);
  assertNotNull(p2);
  assertFalse(ws.has(p2));
  ws.add(p2);

  let p3 = Object.getPrototypeOf(p2);
  assertNotNull(p3);
  assertFalse(ws.has(p3));
  ws.add(p3);
}

{
  let obj1 = {};
  let obj2 = {};
  let obj3 = {};

  let count = 0;
  let p3 = new Proxy(obj3, {
    getPrototypeOf(t) {
      fail `unreachable`;
      if (count++ === 1) {
        Object.setPrototypeOf(obj2, obj1);
      }
      return Reflect.getPrototypeOf(t);
    }
  });
  Object.setPrototypeOf(obj2, p3);
  Object.setPrototypeOf(obj1, obj2);

  assertSame(obj2, Object.getPrototypeOf(obj1));
  assertNotSame(obj1, Object.getPrototypeOf(obj2));
  assertSame(p3, Object.getPrototypeOf(obj2));
}
