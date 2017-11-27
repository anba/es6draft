/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 7.4.8 CreateListIterator: Use standard property definition operations
// https://bugs.ecmascript.org/show_bug.cgi?id=2720

import* as self from "./bug2720.jsm";

// let listIterator = self[Symbol.iterator]();
// let listIteratorNext = listIterator.next;
//
// assertDataProperty(listIterator, "next", {value: listIteratorNext, writable: true, enumerable: false, configurable: true});
