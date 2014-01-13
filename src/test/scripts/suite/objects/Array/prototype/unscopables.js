/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertDataProperty
} = Assert;

// 22.1.3.31 Array.prototype [ @@unscopables ]
const ArrayPrototypeUnscopablesNames = [
  "find",
  "findIndex",
  "fill",
  "copyWithin",
  "entries",
  "keys",
  "values",
];

const ArrayPrototypeUnscopables = Array.prototype[Symbol.unscopables];

assertSame("object", typeof ArrayPrototypeUnscopables);
assertSame(Object.prototype, Object.getPrototypeOf(ArrayPrototypeUnscopables));
assertTrue(Object.isExtensible(ArrayPrototypeUnscopables));

for (let name of ArrayPrototypeUnscopablesNames) {
  assertDataProperty(ArrayPrototypeUnscopables, name, {value: true, writable: true, enumerable: true, configurable: true});
}

assertSame(0, Object.getOwnPropertySymbols(ArrayPrototypeUnscopables).length);
assertSame(0, Object.getOwnPropertyNames(ArrayPrototypeUnscopables).filter(n => ArrayPrototypeUnscopablesNames.indexOf(n) == -1).length);
