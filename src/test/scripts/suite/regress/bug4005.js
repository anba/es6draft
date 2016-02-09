/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  fail
} = Assert;

// 24.2.2.1 DataView: Remove length integer validation and use ToLength ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4005

new DataView(new ArrayBuffer(0), 0, -1);
