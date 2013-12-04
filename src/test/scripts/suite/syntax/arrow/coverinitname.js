/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// CoverInitialisedName in ArrowParameters is a SyntaxError
assertSyntaxError(`({a = 0}) => {};`);
assertSyntaxError(`({a = 0, b = 0}) => {};`);
assertSyntaxError(`({a = 0}, {b = 0}) => {};`);
