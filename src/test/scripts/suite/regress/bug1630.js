/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, fail
} = Assert;

// 12.11.1.2: Wrong evaluation semantics in step 4b
// https://bugs.ecmascript.org/show_bug.cgi?id=1630

{
  let selectorCalled = false;
  function selector() {
    assertFalse(selectorCalled);
    selectorCalled = true;
    return 0;
  }
  let statementEvaluted = false;
  switch (0) {
    case selector(): {
      assertTrue(selectorCalled);
      assertFalse(statementEvaluted);
      statementEvaluted = true;
    }
  }
  assertTrue(selectorCalled);
  assertTrue(statementEvaluted);
}

{
  let selectorCalled = false;
  function selector() {
    assertFalse(selectorCalled);
    selectorCalled = true;
    return 1;
  }
  switch (0) {
    case selector(): {
      fail `Unexpected statement evaluation`;
    }
  }
  assertTrue(selectorCalled);
}
