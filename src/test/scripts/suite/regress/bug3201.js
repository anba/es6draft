/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertEquals, fail
} = Assert;

// 25.3.3.4 GeneratorResumeAbrupt: Missing Return after step 3b
// https://bugs.ecmascript.org/show_bug.cgi?id=3201

function* g() { fail `unreachable`; }
class Err extends Error { }

{
  let gen = g();
  assertEquals({value: 1, done: true}, gen.return(1));
  assertEquals({value: void 0, done: true}, gen.next(2));
}

{
  let gen = g();
  assertEquals({value: 1, done: true}, gen.return(1));
  assertEquals({value: 2, done: true}, gen.return(2));
  assertEquals({value: void 0, done: true}, gen.next(3));
}

{
  let gen = g();
  assertThrows(Err, () => gen.throw(new Err));
  assertEquals({value: void 0, done: true}, gen.next(1));
}

{
  let gen = g();
  assertThrows(Err, () => gen.throw(new Err));
  assertThrows(Err, () => gen.throw(new Err));
  assertEquals({value: void 0, done: true}, gen.next(1));
}
