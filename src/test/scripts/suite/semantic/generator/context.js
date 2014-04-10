/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

"use strict";

const {
  assertSame,
  assertThrows,
} = Assert;

function assertObjectFrom(realm, object) {
  return assertSame(realm.global.Object.prototype, Object.getPrototypeOf(object));
}

function assertThrowsTypeErrorFrom(realm, fn) {
  return assertThrows(fn, realm.global.TypeError);
}

// 25.3.3.2 GeneratorResume
// Test GeneratorResume uses the correct execution context
{
  let foreignRealm = new Reflect.Realm();
  foreignRealm.eval(`
    let TryCatch = f => { try { f() } catch (e) {} };
    let Step = g => (g = g(), g.next(), g);
    let StepTryCatch = g => (g = g(), TryCatch(() => g.next()), g);
    function* Empty() { }
    function* Yield() { yield 0 }
    function* Return() { return 1 }
    function* Executing(f) { f() }
    function SuspendedEmpty() { return Step(function*(){ yield }) }
    function SuspendedYield() { return Step(function*(){ yield; yield 0 }) }
    function SuspendedReturn() { return Step(function*(){ yield; return 1 }) }
    function SuspendedExecuting(f) { return Step(function*(){ yield; f() }) }
    function Completed() { return Step(function*(){ }) }
    function CompletedAbrupt() { return StepTryCatch(function*(){ throw 2 }) }
    function Uninitialised() { return (function*(){})[Symbol.create]() }
    function Invalid() { return {} }
  `);
  let {
    Empty, Yield, Return, Executing,
    SuspendedEmpty, SuspendedYield, SuspendedReturn, SuspendedExecuting,
    Completed, CompletedAbrupt,
    Uninitialised, Invalid
  } = foreignRealm.global;
  let nextRealm = new Reflect.Realm();
  let nextF = nextRealm.eval(`(function*(){})().next`);
  let next = (g, ...args) => nextF.call(g, ...args);
  let Next = (g, ...args) => next(g(), ...args);

  // GeneratorResume for non-generator
  assertThrowsTypeErrorFrom(nextRealm, () => Next(Invalid));

  // GeneratorResume for uninitialised generator
  assertThrowsTypeErrorFrom(nextRealm, () => Next(Uninitialised));

  // GeneratorResume in "suspendedStart", no arguments
  assertObjectFrom(nextRealm, Next(Empty));
  assertObjectFrom(foreignRealm, Next(Yield));
  assertObjectFrom(nextRealm, Next(Return));

  // GeneratorResume in "suspendedStart", argument is undefined
  assertObjectFrom(nextRealm, Next(Empty, void 0));
  assertObjectFrom(foreignRealm, Next(Yield, void 0));
  assertObjectFrom(nextRealm, Next(Return, void 0));

  // GeneratorResume in "suspendedStart", argument is not undefined
  assertObjectFrom(nextRealm, Next(Empty, "not-undefined"));
  assertObjectFrom(foreignRealm, Next(Yield, "not-undefined"));
  assertObjectFrom(nextRealm, Next(Return, "not-undefined"));

  // GeneratorResume in "suspendedYield"
  assertObjectFrom(nextRealm, Next(SuspendedEmpty));
  assertObjectFrom(foreignRealm, Next(SuspendedYield));
  assertObjectFrom(nextRealm, Next(SuspendedReturn));

  // GeneratorResume in "completed"
  assertObjectFrom(nextRealm, Next(Completed));
  assertObjectFrom(nextRealm, Next(CompletedAbrupt));

  // GeneratorResume in "executing"
  assertThrowsTypeErrorFrom(nextRealm, () => {
    let g = Executing(() => next(g));
    return next(g);
  });
  assertThrowsTypeErrorFrom(nextRealm, () => {
    let g = SuspendedExecuting(() => next(g));
    return next(g);
  });
}
