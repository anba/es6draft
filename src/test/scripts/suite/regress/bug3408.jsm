/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertEquals
} = Assert;

// 9.4.6: Missing/Incomplete object internal methods
// https://bugs.ecmascript.org/show_bug.cgi?id=3408

import* as self from "./bug3408.jsm";
export let a = 0;

// [[Get]] with symbol-valued property key
assertSame("Module", self[Symbol.toStringTag]);
assertSame("Module", Reflect.get(self, Symbol.toStringTag));
assertSame("Module", Reflect.get(self, Symbol.toStringTag, self));

// [[HasProperty]] with symbol-valued property key
assertTrue(Symbol.toStringTag in self);
assertTrue(Reflect.has(self, Symbol.toStringTag));

// [[GetOwnProperty]] with symbol-valued property key
assertEquals(
  {value: "Module", writable: false, enumerable: false, configurable: true},
  Reflect.getOwnPropertyDescriptor(self, Symbol.toStringTag)
);

// [[OwnPropertyKeys]]
assertEquals(["a"], Object.keys(self));
assertEquals(["a"], Object.getOwnPropertyNames(self));
assertEquals([Symbol.toStringTag, Symbol.iterator], Object.getOwnPropertySymbols(self));
assertEquals(["a", Symbol.toStringTag, Symbol.iterator], Reflect.ownKeys(self));

// Test [[Exports]] list is copied by executing [[OwnPropertyKeys]] multiple times
assertEquals(["a", Symbol.toStringTag, Symbol.iterator], Reflect.ownKeys(self));
assertEquals(["a", Symbol.toStringTag, Symbol.iterator], Reflect.ownKeys(self));
assertEquals(["a", Symbol.toStringTag, Symbol.iterator], Reflect.ownKeys(self));
