/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 14.5.4 ClassTail, Contains: Contains definition does not take computed property names into account
// https://bugs.ecmascript.org/show_bug.cgi?id=2510

function outer() {
  // use early return to ensure class declaration is not evaluated
  return;
  // `outer` requires a super-binding due to `super.x` in the computed property name;
  class C {
    [super.x](){}
  }
}

assertThrows(ReferenceError, outer);

// No ReferenceError when [[HomeObject]] is set
outer.toMethod({})();
