/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined, assertNotUndefined, assertDataProperty
} = Assert;

// Module exotic object and GetOwnProperty?
// https://bugs.ecmascript.org/show_bug.cgi?id=4083

import* as self from "./bug4083.jsm";
export default 123;
export let foo = 456;

assertDataProperty(self, "default", {value: 123, writable: true, enumerable: true, configurable: false});
assertDataProperty(self, "foo", {value: 456, writable: true, enumerable: true, configurable: false});
assertUndefined(Object.getOwnPropertyDescriptor(self, "bar"));
assertUndefined(Object.getOwnPropertyDescriptor(self, ""));
assertUndefined(Object.getOwnPropertyDescriptor(self, 0));
assertUndefined(Object.getOwnPropertyDescriptor(self, 1));

assertUndefined(Object.getOwnPropertyDescriptor(self, Symbol.iterator));
assertNotUndefined(Object.getOwnPropertyDescriptor(self, Symbol.toStringTag));
assertUndefined(Object.getOwnPropertyDescriptor(self, Symbol()));
assertUndefined(Object.getOwnPropertyDescriptor(self, Symbol.hasInstance));
