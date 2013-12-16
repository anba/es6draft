/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertConstructor, assertNotConstructor,
} = Assert;

// [[Construct]] is assigned to BoundFunction objects if the [[BoundTargetFunction]]
// has [[Construct]] at initialisation time
{
  class Fn extends Function {
    constructor(...args) {
      this.preBound = this.bind(null);
      // [[Construct]] is assigned in super constructor() method
      super(...args);
      this.postBound = this.bind(null);
    }
  }

  let fn = new Fn("");
  assertNotConstructor(fn.preBound);
  assertConstructor(fn.postBound);
}
