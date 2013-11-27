/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

// Only works due to the terrible unicode case hack when emitting back references
assertTrue(/(\u01C6)\1/i.test("\u01C6\u01C5"));

// This should be false instead of true
assertTrue(/(\u0069)\1/i.test("\u0069\u0131"));
