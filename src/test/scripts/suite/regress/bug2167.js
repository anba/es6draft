/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 23.1.5.2.1, 23.2.5.2.1: Move assertion in step 7 after step 8
// https://bugs.ecmascript.org/show_bug.cgi?id=2167

let map = new Map();
let mapIter = map[Symbol.iterator]();
assertEquals({done: true, value: void 0}, mapIter.next());
assertEquals({done: true, value: void 0}, mapIter.next());

let set = new Set();
let setIter = set[Symbol.iterator]();
assertEquals({done: true, value: void 0}, setIter.next());
assertEquals({done: true, value: void 0}, setIter.next());
