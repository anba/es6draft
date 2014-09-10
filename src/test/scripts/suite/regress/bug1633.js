/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 13.4.1.1: Error recovery in delegating yield incomplete
// https://bugs.ecmascript.org/show_bug.cgi?id=1633

let log = "";

function* gen() {
  log += "{start}";
  yield 1;
  log += "{stop}";
}

gen.prototype.throw = function(e) {
  log += "{throw}";
  return {done: false};
};

// Note: Rev27 disabled error recovery

let g = function*(){ yield* gen(); }();
g.next();
// g.throw("Stop!");
let exc; try { g.throw("Stop!"); } catch(e) { exc = e; }
assertSame("Stop!", exc);
g.next();

// assertSame("{start}{throw}{stop}", log);
assertSame("{start}{throw}", log);
