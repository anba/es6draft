/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.4.12 InstantiateFunctionObject: Missing call to SetFunctionName
// https://bugs.ecmascript.org/show_bug.cgi?id=3462

import* as self from "./bug3462.jsm";
import def from "./bug3462.jsm";

export default function*(){};

assertSame("default", self.default.name);
assertSame(self.default, def);
