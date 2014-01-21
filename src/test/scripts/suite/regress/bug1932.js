/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
} = Assert;

// 22.2.1.4: Remove step 15
// https://bugs.ecmascript.org/show_bug.cgi?id=1932

// no error
new Int8Array(new ArrayBuffer(0), 0, 0);
