/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, fail
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

async function* g() {
  try {
    yield 123;
  } finally {
    typeof await new Promise(() => {});
  }
  fail `unreachable`;
}

var it = g();

// Start generator execution, pushes AsyncGeneratorRequest<Normal> to async queue.
// Execution stops at 'yield', AsyncGeneratorRequest<Normal> popped from queue.
it.next(0).then(v => assertEquals({value: 123, done: false}, v)).catch(reportFailure);

// Continue execution after 'yield', pushes AsyncGeneratorRequest<Return> to async queue.
// Execution stops at 'await', async queue unchanged.
it.return(1).then(v => assertEquals({value: 1, done: true}, v)).catch(reportFailure);

// Continue execution after 'await', pushes AsyncGeneratorRequest<Normal> to async queue.
// 'await' is resumed with queue[0] = AsyncGeneratorRequest<Return>.
it.next(2).then(v => assertEquals({value: void 0, done: true}, v)).catch(reportFailure);
