/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 8.4.3.1, 8.4.3.2: undefined [[StringData]] case not handled
// https://bugs.ecmascript.org/show_bug.cgi?id=1414

let str = new class extends String { constructor() { /* no super */ } };
assertUndefined(Object.getOwnPropertyDescriptor(str, "0"));
