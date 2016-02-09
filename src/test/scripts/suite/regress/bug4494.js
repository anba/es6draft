/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

//  14.4.1 Early Errors: Missing rule for "GeneratorDeclaration : function * (..."
// https://bugs.ecmascript.org/show_bug.cgi?id=4494

assertThrows(SyntaxError, () => parseModule(`export default function*() { super() }`));
