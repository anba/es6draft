/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// 19.1.3.6 Object.prototype.toString: Possible to detect proxied array objects
// https://bugs.ecmascript.org/show_bug.cgi?id=3472

var p = new Proxy([], {
 get(t,pk,r){
  if(pk === Symbol.toStringTag) return "Array";
  return Reflect.get(t,pk,r)
}});

assertTrue(Array.isArray(p));
assertSame("[object Array]", Object.prototype.toString.call(p));
