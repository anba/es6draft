/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.2: Optional Arguments in "MemberExpression : new super Arguments" leads to Shift/Reduce conflicts
// https://bugs.ecmascript.org/show_bug.cgi?id=2338

/*
 * MemberExpression :
 *      new super Arguments{opt}
 * CallExpression :
 *      MemberExpression Arguments
 * LeftHandSideExpression :
 *      MemberExpression
 *      CallExpression
 *
 * Problem: `new super()`
 * - `new super()` -> MemberExpression `()` -> CallExpression -> LeftHandSideExpression
 * - `new super()` -> MemberExpression -> LeftHandSideExpression
 */

{
  let inner = 0, outer = 0;
  class Base {
    constructor() {
      outer += 1;
      return () => { inner += 1 };
    }
  }
  new class Derived extends Base {
    constructor() { /* call super to initialize this */ super(); new super }
  }
  assertSame(0, inner);
  assertSame(2, outer);
}

{
  let inner = 0, outer = 0;
  class Base {
    constructor() {
      outer += 1;
      return () => { inner += 1 };
    }
  }
  new class Derived extends Base {
    constructor() { /* call super to initialize this */ super(); new super() }
  }
  assertSame(0, inner);
  assertSame(2, outer);
}

{
  let inner = 0, outer = 0;
  class Base {
    constructor() {
      outer += 1;
      return () => { inner += 1 };
    }
  }
  new class Derived extends Base {
    constructor() { /* call super to initialize this */ super(); (new super)() }
  }
  assertSame(1, inner);
  assertSame(2, outer);
}
