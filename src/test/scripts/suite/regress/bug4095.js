/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// duplicate bindings allowed in VariableDeclaration BindingPattern
// https://bugs.ecmascript.org/show_bug.cgi?id=4095

var {a, a} = {};
assertSame(void 0, a);

var {a, a} = {get a() { return this._a++ }, _a: 0};
assertSame(1, a);

var [b, b] = [];
assertSame(void 0, b);

var [b, b] = [0, 1];
assertSame(1, b);
