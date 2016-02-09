/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertCallable
} = Assert;

// Support default-exporting class declarations
// https://bugs.ecmascript.org/show_bug.cgi?id=3394

import anonymousDefaultClassDecl from "./resources/bug3394_1.jsm";
import namedDefaultClassDecl from "./resources/bug3394_2.jsm";

assertCallable(anonymousDefaultClassDecl);
assertSame("default", anonymousDefaultClassDecl.name);

assertCallable(namedDefaultClassDecl);
assertSame("C", namedDefaultClassDecl.name);
