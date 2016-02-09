/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.2.1.1 Early Errors: Incorrect restriction for ExportedBindings
// https://bugs.ecmascript.org/show_bug.cgi?id=3411

export {a as b} from "./resources/bug3411.jsm";

assertSame("undefined", typeof a);
assertSame("undefined", typeof b);
