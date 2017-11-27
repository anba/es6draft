/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// quick self-test
assertSame("ABCDEFGHIJKLMNOPQRSTUVWXYZ", range('A', 'Z').toString());

export default function range(start, end, increment = 1) {
  assertTrue(typeof start == 'number' || typeof start == 'string' && start.length == 1);
  assertTrue(typeof end == 'number' || typeof end == 'string' && start.length == 1);
  assertTrue(typeof increment == 'number');

  start = typeof start == 'string' ? start.codePointAt(0) : +start;
  end = typeof end == 'string' ? end.codePointAt(0) : +end;
  assertTrue(start <= end);
  assertTrue(increment > 0);

  class Range {
    constructor(start, end, increment) {
      Object.assign(this, {start, end, increment});
      this.string = String.fromCodePoint(...this);
    }

    toString() {
      return this.string;
    }

    startR() {
      return new Range(this.start, this.start, 1);
    }

    endR() {
      return new Range(this.end, this.end, 1);
    }

    [Symbol.iterator]() {
      return new class RangeIterator {
        constructor(range) {
          Object.assign(this, {range, value: range.start});
        }

        next() {
          let value = this.value;
          if (value <= this.range.end) {
            this.value += this.range.increment;
            return {done: false, value};
          }
          return {done: true, value};
        }
      }(this);
    }
  }

  return new Range(start, end, increment);
}
