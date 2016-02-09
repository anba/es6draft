/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.5.16 ClassDeclaration Evaluation: className passed to ClassDefinitionEvaluation for ClassDeclaration
// https://bugs.ecmascript.org/show_bug.cgi?id=2202

{
  class C { f() { return C } }
  let captured = C;
  let c = new C;
  C = null;
  assertSame(captured, c.f());
}
