/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 8.5.7: Step 18b of [[DefineOwnProperty]] needs to test for presence of [[Configurable]]
// https://bugs.ecmascript.org/show_bug.cgi?id=1543

let p = new Proxy({foo: 0}, {
  defineProperty: (...args) => Reflect.defineProperty(...args)
});
Object.defineProperty(p, "foo", {value: 1});

assertDataProperty(p, "foo", {value: 1, writable: true, enumerable: true, configurable: true});
