/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals, assertThrows
} = Assert;

// 7.4.1 GetIterator, 7.4.2 IsIterable: Missing IsCallable checks and typos
// https://bugs.ecmascript.org/show_bug.cgi?id=2672

// Symbol.iterator present and valid iterator
assertEquals([], Array.from({*[Symbol.iterator](){}}));
assertEquals([0], Array.from({*[Symbol.iterator](){ yield 0 }}));

// No error if Symbol.iterator not present or undefined
assertEquals([], Array.from({}));
assertEquals([], Array.from({[Symbol.iterator]: void 0}));

// Throw TypeError if Symbol.iterator is not callable
assertThrows(TypeError, () => Array.from({[Symbol.iterator]: null}));
assertThrows(TypeError, () => Array.from({[Symbol.iterator]: 0}));
assertThrows(TypeError, () => Array.from({[Symbol.iterator]: 1}));
assertThrows(TypeError, () => Array.from({[Symbol.iterator]: {}}));
