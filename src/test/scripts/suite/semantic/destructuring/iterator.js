/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

function countIter(iterable) {
  let counterNext = 0, counterIterator = 0;
  let count = () => counterNext;
  let countIterator = () => counterIterator;
  let source = iterable[Symbol.iterator]();
  let iter = {
    next() {
      counterNext += 1;
      return source.next();
    },
    [Symbol.iterator]() {
      counterIterator += 1;
      return this;
    }
  };
  return {iter, count, countIterator};
}

// Destructuring works with zero elements, still calls @@iterator
{
  let {iter, count, countIterator} = countIter([0, 1, 2]);
  let [] = iter;
  assertSame(0, count());
  assertSame(1, countIterator());
}

// Destructuring multiple elements, same number of source elements
{
  let {iter, count, countIterator} = countIter([0, 1]);
  let [a, b] = iter;
  assertSame(2, count());
  assertSame(1, countIterator());
  assertSame(0, a);
  assertSame(1, b);
}

// Destructuring multiple elements, more source elements
{
  let {iter, count, countIterator} = countIter([0, 1, 2]);
  let [a, b] = iter;
  assertSame(2, count());
  assertSame(1, countIterator());
  assertSame(0, a);
  assertSame(1, b);
}

// Destructuring multiple elements, too few source elements
{
  let {iter, count, countIterator} = countIter([0]);
  let [a, b] = iter;
  assertSame(2, count());
  assertSame(1, countIterator());
  assertSame(0, a);
  assertSame(void 0, b);
}

// Destructuring with elision in front
{
  let {iter, count, countIterator} = countIter([0, 1, 2]);
  let [, a] = iter;
  assertSame(2, count());
  assertSame(1, countIterator());
  assertSame(1, a);
}

// Destructuring with elision in back
{
  let {iter, count, countIterator} = countIter([0, 1, 2]);
  let [a, ,] = iter;
  assertSame(2, count());
  assertSame(1, countIterator());
  assertSame(0, a);
}
