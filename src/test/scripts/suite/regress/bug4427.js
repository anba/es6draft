/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Annex B.3.3 needs to look at all blocks, not VarScopedDeclarations
// https://bugs.ecmascript.org/show_bug.cgi?id=4427

function F() {
  { function nonInVarDecl() { return 1; } }
  return nonInVarDecl();
}

assertSame(1, F());
