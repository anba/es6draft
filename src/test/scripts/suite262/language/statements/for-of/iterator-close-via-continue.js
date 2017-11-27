/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
description: >
    Iterators should be closed via their `return` method when iteration is
    interrupted via a `continue` statement.
features: [Symbol.iterator]
---*/

var startedCount = 0;
var returnCount = 0;
var iterationCount = 0;
var iterable = {};

iterable[Symbol.iterator] = function() {
  return {
    next: function() {
      startedCount += 1;
      return { done: false, value: null };
    },
    return: function() {
      returnCount += 1;
      return {};
    }
  };
};

L: do {
  for (var x of iterable) {
    assert.sameValue(
      startedCount, 1, 'Value is retrieved'
    );
    assert.sameValue(
      returnCount, 0, 'Iterator is not closed'
    );
    iterationCount += 1;
    continue L;
  }
} while (false);

assert.sameValue(
  startedCount, 1, 'Iterator does not restart following interruption'
);
assert.sameValue(iterationCount, 1, 'A single iteration occurs');
assert.sameValue(
  returnCount, 1, 'Iterator is closed after `continue` statement'
);
