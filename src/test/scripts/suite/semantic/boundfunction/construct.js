/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertConstructor,
} = Assert;

// [[Construct]] is assigned to BoundFunction objects if the [[BoundTargetFunction]]
// has [[Construct]] at initialisation time
{
  class Fn extends Function {
    constructor(...args) {
      // [[Construct]] was already assigned when allocated
      super(...args);
      this.postBound = this.bind(null);
    }
  }

  let fn = new Fn("");
  assertConstructor(fn.postBound);
}
