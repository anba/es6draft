/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.1.2.1: Lexical declarations should always get into a new declarative environment
// https://bugs.ecmascript.org/show_bug.cgi?id=1788

{
  let x = 0;
  { /* block scope */
    eval("let x = 1");
    assertSame(0, x);
  }
  assertSame(0, x);
}

function testBlock() {
  let x = 0;
  { /* block scope */
    eval("let x = 1");
    assertSame(0, x);
  }
  assertSame(0, x);
}
testBlock();

function testBlockInner() {
  { /* block scope */
    let x = 0;
    { /* block scope */
      eval("let x = 1");
      assertSame(0, x);
    }
    assertSame(0, x);
  }
  assertSame("undefined", typeof x);
}
testBlockInner();

{
  let o = {};
  with(o) eval("let x = 1");
  assertSame(void 0, o.x);
  assertSame("undefined", typeof x);
}

function testWith() {
  let o = {};
  with(o) eval("let x = 1");
  assertSame(void 0, o.x);
  assertSame("undefined", typeof x);
}
testWith();
