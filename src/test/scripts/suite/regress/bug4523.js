/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 26.2.2.1.1 Proxy Revocation Functions: Length property value not defined
// https://bugs.ecmascript.org/show_bug.cgi?id=4523

var {revoke} = Proxy.revocable({}, {});
assertSame(0, revoke.length);
