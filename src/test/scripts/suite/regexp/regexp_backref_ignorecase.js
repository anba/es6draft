/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

// Test back references are working if ignoreCase is set.

assertTrue(/(\u01C6)\1/i.test("\u01C6\u01C5"));
assertFalse(/(\u0069)\1/i.test("\u0069\u0131"));
