/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 9.5.6: No TypeError thrown in step 21b if [[Configurable]] field not present
// https://bugs.ecmascript.org/show_bug.cgi?id=2297

let proxy = new Proxy({}, { defineProperty(){ return true } });
Object.defineProperty(proxy, "p", {}); // no error here, and no property was created
assertUndefined(Object.getOwnPropertyDescriptor(proxy, "p"));
