/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 13.2.1.1 BindingIdentifier, Early Errors: Apply StringValue Identifier normalisation for BindingIdentifier
// https://bugs.ecmascript.org/show_bug.cgi?id=2521

assertSyntaxError(`var \\u0066unction`);
