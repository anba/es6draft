/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 20.2.2.28 Math.round: Incorrect information in NOTE 2
// https://bugs.ecmascript.org/show_bug.cgi?id=2874

assertSame(0, Math.round(0.499999999999999944488848768742172978818416595458984375));
