/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 9.5.6: No TypeError thrown in step 21b if [[Configurable]] field not present
// https://bugs.ecmascript.org/show_bug.cgi?id=2297

let proxy = new Proxy({}, { defineProperty(){ return true } });
assertThrows(() => Object.defineProperty(proxy, "p", {}), TypeError);
