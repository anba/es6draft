/*
 * Copyright (c) 2012-2015 André Bargull
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
      // [[Construct]] was assigned when allocated
      super(...args);
      this.postProxy = new Proxy(this, {});
    }
  }

  let fn = new Fn("");
  assertConstructor(fn.preProxy);
  assertConstructor(fn.postProxy);
}
