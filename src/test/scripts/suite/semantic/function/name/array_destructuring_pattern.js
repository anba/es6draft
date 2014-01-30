/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse
} = Assert;

function assertAnonymousFunction(f) {
  return assertFalse(f.hasOwnProperty("name"));
}


// anonymous function/generator/arrow/class expression in array destructuring assignment are not renamed
(function() {
  // 12.13.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
  // - AssignmentElement[Yield] : DestructuringAssignmentTarget Initialiser{opt}
  var f4, g4, a4, c4, d4;

  [f4 = function (){}] = [];
  assertAnonymousFunction(f4);

  [g4 = function* (){}] = [];
  assertAnonymousFunction(g4);

  [a4 = () => {}] = [];
  assertAnonymousFunction(a4);

  [c4 = class {}] = [];
  assertAnonymousFunction(c4);
})();
