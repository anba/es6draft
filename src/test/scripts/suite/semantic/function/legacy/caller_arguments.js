/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals, assertSame, assertTrue, assertFalse
} = Assert;

const GeneratorFunction = (function*(){}).constructor;

// Uninitialized functions and generators
{
  let f = new class extends Function { constructor(){ /* no super */ } };
  assertTrue(f.hasOwnProperty("caller"));
  assertTrue(f.hasOwnProperty("arguments"));

  let g = new class extends GeneratorFunction { constructor(){ /* no super */ } };
  assertFalse(g.hasOwnProperty("caller"));
  assertFalse(g.hasOwnProperty("arguments"));
}

// Initialized functions and generators
{
  let nonStrictF = Function("");
  assertTrue(nonStrictF.hasOwnProperty("caller"));
  assertTrue(nonStrictF.hasOwnProperty("arguments"));

  let strictF = Function("'use strict'");
  assertFalse(strictF.hasOwnProperty("caller"));
  assertFalse(strictF.hasOwnProperty("arguments"));

  let nonStrictG = GeneratorFunction("");
  assertFalse(nonStrictG.hasOwnProperty("caller"));
  assertFalse(nonStrictG.hasOwnProperty("arguments"));

  let strictG = GeneratorFunction("'use strict'");
  assertFalse(strictG.hasOwnProperty("caller"));
  assertFalse(strictG.hasOwnProperty("arguments"));
}

// Initialized functions and generators (2)
{
  let nonStrictF = Function.call(new class extends Function { constructor(){ /* no super */ } }, "");
  assertTrue(nonStrictF.hasOwnProperty("caller"));
  assertTrue(nonStrictF.hasOwnProperty("arguments"));

  let strictF = Function.call(new class extends Function { constructor(){ /* no super */ } }, "'use strict'");
  assertFalse(strictF.hasOwnProperty("caller"));
  assertFalse(strictF.hasOwnProperty("arguments"));

  let nonStrictG = GeneratorFunction.call(new class extends GeneratorFunction { constructor(){ /* no super */ } }, "");
  assertFalse(nonStrictG.hasOwnProperty("caller"));
  assertFalse(nonStrictG.hasOwnProperty("arguments"));

  let strictG = GeneratorFunction.call(new class extends GeneratorFunction { constructor(){ /* no super */ } }, "'use strict'");
  assertFalse(strictG.hasOwnProperty("caller"));
  assertFalse(strictG.hasOwnProperty("arguments"));
}

// FunctionDeclaration and FunctionExpression
{
  function nonStrictFD() {}
  assertTrue(nonStrictFD.hasOwnProperty("caller"));
  assertTrue(nonStrictFD.hasOwnProperty("arguments"));

  function strictFD() { "use strict" }
  assertFalse(strictFD.hasOwnProperty("caller"));
  assertFalse(strictFD.hasOwnProperty("arguments"));

  let nonStrictFE = function() {};
  assertTrue(nonStrictFE.hasOwnProperty("caller"));
  assertTrue(nonStrictFE.hasOwnProperty("arguments"));

  let strictFE = function() { "use strict" };
  assertFalse(strictFE.hasOwnProperty("caller"));
  assertFalse(strictFE.hasOwnProperty("arguments"));
}

// GeneratorDeclaration and GeneratorExpression
{
  function* nonStrictGD() {}
  assertFalse(nonStrictGD.hasOwnProperty("caller"));
  assertFalse(nonStrictGD.hasOwnProperty("arguments"));

  function* strictGD() { "use strict" }
  assertFalse(strictGD.hasOwnProperty("caller"));
  assertFalse(strictGD.hasOwnProperty("arguments"));

  let nonStrictGE = function*() {};
  assertFalse(nonStrictGE.hasOwnProperty("caller"));
  assertFalse(nonStrictGE.hasOwnProperty("arguments"));

  let strictGE = function*() { "use strict" };
  assertFalse(strictGE.hasOwnProperty("caller"));
  assertFalse(strictGE.hasOwnProperty("arguments"));
}

// ArrowFunction
{
  let nonStrict = () => {};
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = () => { "use strict" };
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// MethodDefinition
{
  let nonStrict = {m(){}}.m;
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = {m(){ "use strict" }}.m;
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// MethodDefinition - Getter
{
  let nonStrict = Object.getOwnPropertyDescriptor({get p(){}}, "p").get;
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = Object.getOwnPropertyDescriptor({get p(){ "use strict" }}, "p").get;
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// MethodDefinition - Setter
{
  let nonStrict = Object.getOwnPropertyDescriptor({set p(x){}}, "p").set;
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = Object.getOwnPropertyDescriptor({set p(x){ "use strict" }}, "p").set;
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// MethodDefinition - GeneratorMethod
{
  let nonStrict = {*m(){}}.m;
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = {*m(){ "use strict" }}.m;
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// ClassDeclaration and ClassExpression
{
  class C {}
  assertFalse(C.hasOwnProperty("caller"));
  assertFalse(C.hasOwnProperty("arguments"));

  let D = class {};
  assertFalse(D.hasOwnProperty("caller"));
  assertFalse(D.hasOwnProperty("arguments"));
}

// Bound function
{
  let nonStrict = function(){}.bind(null);
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = function(){ "use strict" }.bind(null);
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// Builtin function
{
  let builtin = Object.create;
  assertFalse(builtin.hasOwnProperty("caller"));
  assertFalse(builtin.hasOwnProperty("arguments"));
}

// Cloned functions
{
  let nonStrict = function(){}.toMethod({});
  assertFalse(nonStrict.hasOwnProperty("caller"));
  assertFalse(nonStrict.hasOwnProperty("arguments"));

  let strict = function(){ "use strict" }.toMethod({});
  assertFalse(strict.hasOwnProperty("caller"));
  assertFalse(strict.hasOwnProperty("arguments"));
}

// Uninitialized, cloned functions and generators
{
  let f = new class extends Function { constructor(){ /* no super */ } }().toMethod({});
  assertFalse(f.hasOwnProperty("caller"));
  assertFalse(f.hasOwnProperty("arguments"));

  let g = new class extends GeneratorFunction { constructor(){ /* no super */ } }().toMethod({});
  assertFalse(g.hasOwnProperty("caller"));
  assertFalse(g.hasOwnProperty("arguments"));
}
