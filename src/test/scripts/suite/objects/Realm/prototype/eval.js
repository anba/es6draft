/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertBuiltinFunction,
} = Assert;

/* 26.2.3.2 Reflect.Realm.prototype.eval (source) */

const evalFn = Reflect.Realm.prototype.eval;

assertBuiltinFunction(evalFn, "eval", 1);

// steps 1-2 - TypeError if thisValue is not an object
{
  let primitives = [void 0, true, false, 0, 1, 0.1, 0 / 0, "", "abc", Symbol()];
  for (let v of primitives) {
    assertThrows(TypeError, () => evalFn.call(v));
  }
}

// steps 1-2 - TypeError if thisValue is not a Realm object
{
  let objects = [{}, [], Object, Object.create, function(){}, () => {}];
  for (let v of objects) {
    assertThrows(TypeError, () => evalFn.call(v));
  }
}

// steps 3-4 - TypeError if thisValue is an uninitialized Realm object
{
  assertThrows(TypeError, () => evalFn.call(Reflect.Realm[Symbol.create]()));
}

// step 5 - Evaluates an eval script in the given realm (TODO)
{
  // [Specification for IndirectEval() missing]
}
