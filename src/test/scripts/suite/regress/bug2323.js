/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertSyntaxError
} = Assert;

// 13.1.*: Behaviour for top-level GeneratorDeclarations unclear
// https://bugs.ecmascript.org/show_bug.cgi?id=2323

// Duplicate generator declaration on global top-level is allowed
function* globalDecl() { }
function* globalDecl() { }
assertTrue("globalDecl" in this);
assertSame("function", typeof globalDecl);

// Duplicate generator declaration on function top-level is allowed
(function testDuplicateDeclaration() {
  function* gen() { }
  function* gen() { }
  assertSame("function", typeof gen);
})();

assertSyntaxError(`
  {
    // Duplicate declaration on block level not allowed
    function* gen() { }
    function* gen() { }
  }
`);
