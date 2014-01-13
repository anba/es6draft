/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertConstructor, assertNotConstructor,
} = Assert;

// [[Construct]] is assigned to ExoticProxy objects if the [[ProxyTarget]]
// has [[Construct]] at initialisation time
{
  class Fn extends Function {
    constructor(...args) {
      this.preProxy = new Proxy(this, {});
      // [[Construct]] is assigned in super constructor() method
      super(...args);
      this.postProxy = new Proxy(this, {});
    }
  }

  let fn = new Fn("");
  assertNotConstructor(fn.preProxy);
  assertConstructor(fn.postProxy);
}
