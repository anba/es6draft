/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 8.1.1.5.5 CreateImportBinding: Incorrect assert in step 4
// https://bugs.ecmascript.org/show_bug.cgi?id=3479

import {x as y} from "./bug3479.jsm";

export let x = 0;
