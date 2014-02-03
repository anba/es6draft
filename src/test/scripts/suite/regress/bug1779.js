/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals
} = Assert;

// 15.16.5.2.2: step 9.d.ii does not return iteration-result-object
// https://bugs.ecmascript.org/show_bug.cgi?id=1779

assertEquals({value: "a", done: false}, new Set("a").keys().next());
