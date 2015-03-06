/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertNotSame,
  assertTrue,
  assertFalse,
  assertUndefined,
  assertThrows,
} = Assert;

const global = this;

// non-strict and strict generators, generator called without `new`
{
  function* nonStrictGenerator() { yield this; }
  assertSame(global, nonStrictGenerator().next().value);

  function* strictGenerator() { "use strict"; yield this; }
  assertUndefined(strictGenerator().next().value);
}

// non-strict and strict generators, generator called with `new`
{
  function* nonStrictGenerator() { yield this; }
  let g1 = new nonStrictGenerator();
  assertThrows(ReferenceError, () => g1.next());

  function* strictGenerator() { "use strict"; yield this; }
  let g2 = new strictGenerator();
  assertThrows(ReferenceError, () => g2.next());
}

// GeneratorFunction subclass, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  class G extends GeneratorFunction { }

  let g = new G("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

// GeneratorFunction subclass+cloned, clone .prototype set to %Generator%, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  let GeneratorFunctionClone = GeneratorFunction.toMethod({});
  Object.defineProperty(GeneratorFunctionClone, "prototype", {
    value: GeneratorFunction.prototype
  });
  class G extends GeneratorFunctionClone { }

  let g = new G("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

// GeneratorFunction subclass+cloned, clone .prototype set to %Array%, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  let GeneratorFunctionClone = GeneratorFunction.toMethod({});
  Object.defineProperty(GeneratorFunctionClone, "prototype", {
    value: Array
  });
  class G extends GeneratorFunctionClone { }

  let g = new G("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

// GeneratorFunction subclass+cloned, clone .prototype set to %Generator%, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  let GeneratorFunctionClone = GeneratorFunction.toMethod({});
  Object.defineProperty(GeneratorFunctionClone, "prototype", {
    value: GeneratorFunction.prototype
  });
  class G extends GeneratorFunctionClone { }
  let GClone = G.toMethod({});

  let g = new GClone("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

// GeneratorFunction subclass+cloned, clone .prototype set to %Array%, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  let GeneratorFunctionClone = GeneratorFunction.toMethod({});
  Object.defineProperty(GeneratorFunctionClone, "prototype", {
    value: Array
  });
  class G extends GeneratorFunctionClone { }
  let GClone = G.toMethod({});
  Object.defineProperty(GClone, "prototype", {
    value: G.prototype
  });

  let g = new GClone("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

// GeneratorFunction subclass+cloned, clone .prototype set to %Array%, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  let GeneratorFunctionClone = GeneratorFunction.toMethod({});
  Object.defineProperty(GeneratorFunctionClone, "prototype", {
    value: Array
  });
  class G extends GeneratorFunctionClone { }
  let GClone = G.toMethod({});

  let g = new GClone("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

{
  const GeneratorFunction = (function*(){}).constructor;

  class G extends GeneratorFunction { }
  let GClone = G.toMethod({});
  Object.defineProperty(GClone, "prototype", {
    value: Array
  });

  let g = new GClone("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}

{
  const GeneratorFunction = (function*(){}).constructor;

  let GeneratorFunctionClone = GeneratorFunction.toMethod({});
  class G extends GeneratorFunctionClone { }
  let GClone = G.toMethod({});
  Object.defineProperty(GClone, "prototype", {
    value: Array
  });

  let g = new GClone("yield this");
  let gen = new g();
  assertThrows(ReferenceError, () => gen.next());
}
