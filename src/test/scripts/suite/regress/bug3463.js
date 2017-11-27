/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.14.5.3 IteratorDestructuringAssignmentEvaluation: Evaluation order
// https://bugs.ecmascript.org/show_bug.cgi?id=3463

(function() {
  var a, b, c;
  var o = {};

  with (o) {
    ([a] = function*(){ o.a = 0; yield 1; }());
    ([...b] = function*(){ o.b = 0; yield 1; }());
    ({c} = {get c(){ o.c = 0; return 1 }});
  }

  assertSame(1, a);
  assertSame(0, o.a);

  assertSame(1, b[0]);
  assertSame(0, o.b);

  assertSame(1, c);
  assertSame(0, o.c);
})();

(function() {
  var o = {};

  with (o) {
    var [A] = function*(){ o.A = 0; yield 1; }();
    var [...B] = function*(){ o.B = 0; yield 1; }();
    var {C} = {get C(){ o.C = 0; return 1 }};
  }

  assertSame(1, A);
  assertSame(0, o.A);

  assertSame(1, B[0]);
  assertSame(0, o.B);

  assertSame(1, C);
  assertSame(0, o.C);
})();
