/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals
} = Assert;

// 25.2.1.1 GeneratorFunction: Allow GeneratorFunction without YieldExpression
// https://bugs.ecmascript.org/show_bug.cgi?id=2200

assertEquals({value: void 0, done: true}, (function*(){}).constructor("")().next());
