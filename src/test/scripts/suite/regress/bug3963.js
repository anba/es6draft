/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSyntaxError
} = Assert;

// 8.1.2.4, 9.2.11, 14.1.*, 14.2.*, 14.4.*: Remove NeedsSuperBinding
// https://bugs.ecmascript.org/show_bug.cgi?id=3963

const GeneratorFunction = function*(){}.constructor;

assertSyntaxError(`function f() { super.property }`);
assertSyntaxError(`function* f() { super.property }`);
assertThrows(SyntaxError, () => Function("super.property"));
assertThrows(SyntaxError, () => GeneratorFunction("super.property"));
