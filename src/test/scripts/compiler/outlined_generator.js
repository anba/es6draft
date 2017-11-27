/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const GeneratorFunction = function*(){}.constructor;

function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function testWith(g) {
  var log = [];
  var it = g(log);

  assertEq(log.length, 0);

  var res = it.next();
  assertEq(log.length, 1);
  assertEq(log[0], "before yield");
  assertEq(res.value, 1);
  assertEq(res.done, false);

  var res = it.next();
  assertEq(log.length, 2);
  assertEq(log[1], "after yield");
  assertEq(res.value, void 0);
  assertEq(res.done, true);
}

testWith(GeneratorFunction("log", `
  void { ${"a:0,".repeat(1000)} b: log.push("before yield"), c: yield 1, d: log.push("after yield") };
`));
testWith(GeneratorFunction("log", `
  void [ ${"0,".repeat(1000)} log.push("before yield"), yield 1, log.push("after yield") ];
`));
testWith(GeneratorFunction("log", `
  void ( ${"0,".repeat(1000)} log.push("before yield"), yield 1, log.push("after yield") );
`));
testWith(GeneratorFunction("log", `
  void class { ${"a(){};".repeat(200)} [log.push("before yield")](){}; [(yield 1)](){}; [log.push("after yield")](){}; };
`));
