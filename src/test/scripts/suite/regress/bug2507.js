/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 13.6.4.1 for-in/of early errors: Missing restriction to disallow "let" in ForDeclaration
// https://bugs.ecmascript.org/show_bug.cgi?id=2507

assertSyntaxError(`
  for (let let in []) ;
`);

assertSyntaxError(`
  for (let let of []) ;
`);
