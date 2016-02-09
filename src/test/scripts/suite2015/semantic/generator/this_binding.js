/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows,
} = Assert;

const global = this;

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
