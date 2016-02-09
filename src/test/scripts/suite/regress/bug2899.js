/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
} = Assert;

// 19.2.1.1 Function, 25.2.1.1 GeneratorFunction: Remove step 10
// https://bugs.ecmascript.org/show_bug.cgi?id=2899

const Generator = (function*(){}).constructor;

Function("a = 0", "var a = 1");
Generator("a = 0", "var a = 1");
