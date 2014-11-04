/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

// Re-export assert module as global property "Assert".
Object.defineProperty(this, "Assert", {value: System.get("lib/assert.jsm")});
