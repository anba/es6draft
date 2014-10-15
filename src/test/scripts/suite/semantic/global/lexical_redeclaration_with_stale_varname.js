/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse,
  assertTrue,
  assertThrows,
  assertSame,
} = Assert;


function testLexicalRedeclaration(global) {
  // Create global var-declared name.
  (1, eval)("var x1 = 1");
  assertSame(1, x1);
  assertSame(1, global.x1);

  // Attempt to redeclare as lexical throws a SyntaxError.
  assertThrows(SyntaxError, () => (1, eval)("let x1 = 2"));

  // Assert values were not altered.
  assertSame(1, x1);
  assertSame(1, global.x1);
}
testLexicalRedeclaration(this);


function testDeleteGlobalRef(global) {
  // Create global var-declared name.
  (1, eval)("var x2 = 1");

  // Delete global var-declared name with unqualified delete.
  assertTrue(delete x2);

  // Redeclaration as lexical is now allowed.
  (1, eval)("let x2 = 2");
  assertSame(2, x2);
  assertFalse("x2" in global);
}
testDeleteGlobalRef(this);


function testStaleVarName(global) {
  // Create global var-declared name.
  (1, eval)("var x3 = 1");

  // Delete global var-declared name with qualified delete.
  assertTrue(delete global.x3);

  // Attempt to redeclare as lexical throws a SyntaxError; varNames list in
  // global environment record still holds an entry for "x3".
  assertThrows(SyntaxError, () => (1, eval)("let x3 = 2"));

  // Assert values were not altered.
  assertSame("undefined", typeof x3);
  assertFalse("x3" in global);
}
testStaleVarName(this);


function testPurgeVarName(global) {
  // Same set-up as in testStaleVarName.
  (1, eval)("var x4 = 1");
  assertTrue(delete global.x4);

  // Purge varNames entry for "x4" by adding a global property and then
  // performing an unqualified delete.
  global.x4 = -1;
  assertTrue(delete x4);

  // Redeclaration as lexical is now allowed.
  (1, eval)("let x4 = 2");
  assertSame(2, x4);
  assertFalse("x4" in global);
}
testPurgeVarName(this);
