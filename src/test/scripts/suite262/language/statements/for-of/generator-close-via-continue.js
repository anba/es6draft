/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
description: >
    Generators should be closed via their `return` method when iteration is
    interrupted via a `continue` statement.
features: [generators]
---*/

var startedCount = 0;
var finallyCount = 0;
var iterationCount = 0;
function* values() {
  startedCount += 1;
  try {
    yield;
    $ERROR('This code is unreachable (within `try` block)');
  } finally {
    finallyCount += 1;
  }
  $ERROR('This code is unreachable (following `try` statement)');
}
var iterable = values();

assert.sameValue(
  startedCount, 0, 'Generator is initialized in suspended state'
);

L: do {
  for (var x of iterable) {
    assert.sameValue(
      startedCount, 1, 'Generator executes prior to first iteration'
    );
    assert.sameValue(
      finallyCount, 0, 'Generator is paused during first iteration'
    );
    iterationCount += 1;
    continue L;
  }
} while (false);

assert.sameValue(
  startedCount, 1, 'Generator does not restart following interruption'
);
assert.sameValue(iterationCount, 1, 'A single iteration occurs');
assert.sameValue(
  finallyCount, 1, 'Generator is closed after `continue` statement'
);
