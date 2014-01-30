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


// anonymous function/generator/arrow/class expression in object destructuring assignment are not renamed
(function() {
  // 12.13.5.4 Runtime Semantics: KeyedDestructuringAssignmentEvaluation
  // - AssignmentElement[Yield] : DestructuringAssignmentTarget Initialiser{opt}
  var f7, g7, a7, c7;
  var f10, g10, a10, c10;
  var f13, g13, a13, c13;

  ({f7 = function (){}}) = [];
  assertAnonymousFunction(f7);

  ({g7 = function* (){}}) = [];
  assertAnonymousFunction(g7);

  ({a7 = () => {}}) = [];
  assertAnonymousFunction(a7);

  ({c7 = class {}}) = [];
  assertAnonymousFunction(c7);

  ({key: f10 = function (){}}) = [];
  assertAnonymousFunction(f10);

  ({key: g10 = function* (){}}) = [];
  assertAnonymousFunction(g10);

  ({key: a10 = () => {}}) = [];
  assertAnonymousFunction(a10);

  ({key: c10 = class {}}) = [];
  assertAnonymousFunction(c10);

  ({["key"]: f13 = function (){}}) = [];
  assertAnonymousFunction(f13);

  ({["key"]: g13 = function* (){}}) = [];
  assertAnonymousFunction(g13);

  ({["key"]: a13 = () => {}}) = [];
  assertAnonymousFunction(a13);

  ({["key"]: c13 = class {}}) = [];
  assertAnonymousFunction(c13);
})();
