/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined, assertNotUndefined, assertThrows
} = Assert;

// Module exotic object and GetOwnProperty?
// https://bugs.ecmascript.org/show_bug.cgi?id=4083

import* as self from "./bug4083.jsm";
export default 123;
export let foo = 456;

assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, "default"));
assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, "foo"));
assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, "bar"));
assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, ""));
assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, 0));
assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, 1));

assertNotUndefined(Object.getOwnPropertyDescriptor(self, Symbol.iterator));
assertNotUndefined(Object.getOwnPropertyDescriptor(self, Symbol.toStringTag));
assertUndefined(Object.getOwnPropertyDescriptor(self, Symbol()));
assertUndefined(Object.getOwnPropertyDescriptor(self, Symbol.hasInstance));
