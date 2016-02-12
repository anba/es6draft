/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, fail
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

{
  async function* g() {
    await Promise.resolve();
    return -1;
  }

  let it = g();

  // Start generator execution, pushes AsyncGeneratorRequest<Normal, 1> to async queue.
  // Execution stops at 'await', async queue unchanged.
  it.next(1).then(v => assertEquals({value: -1, done: true}, v)).catch(reportFailure);

  // Try to resume generator execution, pushes AsyncGeneratorRequest<Normal, 2> to async queue.
  // 'await' is resumed in next tick, generators returns and pops AsyncGeneratorRequest<Normal, 1> from queue.
  // Second next() invocation receives default `{value: undefined, done: true}` completion.
  it.next(2).then(v => assertEquals({value: void 0, done: true}, v)).catch(reportFailure);
}

{
  async function* g() {
    let x = await Promise.resolve(123);
    assertSame(123, x);

    let y = yield 456;
    assertSame(2, y);

    return -1;
  }

  let it = g();

  // Start generator execution, pushes AsyncGeneratorRequest<Normal, 1> to async queue.
  // Execution stops at 'await', async queue unchanged.
  it.next(1).then(v => assertEquals({value: 456, done: false}, v)).catch(reportFailure);

  // Try to resume generator execution, pushes AsyncGeneratorRequest<Normal, 2> to async queue.
  // 'await' is resumed in next tick.
  // Execution stops at 'yield', AsyncGeneratorRequest<Normal, 1> popped from queue.
  // 'yield' is resumed with queue[0] = AsyncGeneratorRequest<Normal, 2>.
  it.next(2).then(v => assertEquals({value: -1, done: true}, v)).catch(reportFailure);
}
