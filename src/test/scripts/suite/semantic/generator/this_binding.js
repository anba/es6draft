/*
 * Copyright (c) André Bargull
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
  assertThrows(TypeError, () => new nonStrictGenerator());

  function* strictGenerator() { "use strict"; yield this; }
  assertThrows(TypeError, () => new strictGenerator());
}

// GeneratorFunction subclass, generator called with `new`
{
  const GeneratorFunction = (function*(){}).constructor;

  class G extends GeneratorFunction { }

  let g = new G("yield this");
  assertThrows(TypeError, () => new g());
}
