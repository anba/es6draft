/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

function countIter(iterable) {
  let counterNext = 0, counterIterator = 0;
  let count = () => counterNext;
  let countIterator = () => counterIterator;
  let source = iterable[Symbol.iterator]();
  let iter = {
    next() {
      counterNext += 1;
      let result = source.next();
      if (result.done) {
        return {done: true, get value() {
          fail `Called .value on finished iterator`;
        }};
      }
      return result;
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

// Destructuring multiple elements, too few source elements
{
  let {iter, count, countIterator} = countIter([]);
  let [a, b] = iter;
  assertSame(1, count());
  assertSame(1, countIterator());
  assertSame(void 0, a);
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

// Destructuring does not use value from termination result
{
  let {iter, count, countIterator} = countIter(function*(){ yield 0; return 1; }());
  let [a, b] = iter;
  assertSame(2, count());
  assertSame(1, countIterator());
  assertSame(0, a);
  assertSame(void 0, b);
}

// Destructuring does not use value from termination result
{
  let {iter, count, countIterator} = countIter(function*(){ return 0; }());
  let [a, b] = iter;
  assertSame(1, count());
  assertSame(1, countIterator());
  assertSame(void 0, a);
  assertSame(void 0, b);
}
