/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertThrows
} = Assert;

// 7.4.10 CreateCompoundIterator: Investigate to make CompoundIterator indistinguishable from ListIterator
// https://bugs.ecmascript.org/show_bug.cgi?id=2957

let listIter = Reflect.enumerate({a: 0, b: 1});
let compoundIter = Reflect.enumerate(new String("ab"));
let otherListIter = Reflect.enumerate({});
let otherCompoundIter = Reflect.enumerate(new String(""));

// Works on same instance
assertEquals({value: "a", done: false}, listIter.next.call(listIter));
assertEquals({value: "0", done: false}, compoundIter.next.call(compoundIter));

// Does not work on different instance
assertThrows(TypeError, () => listIter.next.call(compoundIter));
assertThrows(TypeError, () => compoundIter.next.call(listIter));
assertThrows(TypeError, () => listIter.next.call(otherListIter));
assertThrows(TypeError, () => compoundIter.next.call(otherCompoundIter));

// Drain iters
assertEquals({value: "b", done: false}, listIter.next.call(listIter));
assertEquals({value: void 0, done: true}, listIter.next.call(listIter));
assertEquals({value: "1", done: false}, compoundIter.next.call(compoundIter));
assertEquals({value: void 0, done: true}, compoundIter.next.call(compoundIter));
