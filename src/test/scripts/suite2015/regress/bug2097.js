/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty, assertBuiltinFunction
} = Assert;

// 7.4.7 CreateListIterator, step 4: underspecified "define as own property"
// https://bugs.ecmascript.org/show_bug.cgi?id=2097

let listIter = Reflect.enumerate({});
assertDataProperty(listIter, "next", {value: listIter.next, writable: true, enumerable: false, configurable: true});
assertBuiltinFunction(listIter.next, "next", 0);
