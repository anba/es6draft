/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// Why can't the default export be exported with export *?
// https://bugs.ecmascript.org/show_bug.cgi?id=3292

import * as mod from "./resources/bug3292_1.jsm";

assertEquals([], Object.getOwnPropertyNames(mod));
assertEquals([], [...Reflect.enumerate(mod)]);
