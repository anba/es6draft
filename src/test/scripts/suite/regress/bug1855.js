/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertTrue
} = Assert;

// 8.5.4: [[PreventExtensions]] returns target [[Extensible]] instead of trap result
// https://bugs.ecmascript.org/show_bug.cgi?id=1855

let target = {};
let proxy = new Proxy(target, {preventExtensions(){ return false }});
assertTrue(Object.isExtensible(target));
assertThrows(TypeError, () => Object.preventExtensions(proxy));
assertTrue(Object.isExtensible(target));
