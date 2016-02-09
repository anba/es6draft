/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// [[HomeObject]] is not bound to [[Prototype]]
{
  let log = "";

  class Base {
    p() { log += "[Base]"; }
  }
  class OtherBase {
    p() { log += "[OtherBase]"; }
  }
  class Derived extends Base {
    p() {
      log += "[Derived]";
      super.p();
      Object.setPrototypeOf(Derived.prototype, OtherBase.prototype);
      super.p();
    }
  }
  (new Derived).p();

  assertSame("[Derived][Base][OtherBase]", log);
}
