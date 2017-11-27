/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// ClassHeritage : extends LeftHandSideExpression

assertSyntaxError(`class C extends G || H {}`);
assertSyntaxError(`class C extends c ? G : H {}`);
assertSyntaxError(`function* g(){ class C extends yield 0 {} }`);

function testparse() {
  class C extends F() {}

  function* g() {
    class C extends (yield 0) {}
  }
}
