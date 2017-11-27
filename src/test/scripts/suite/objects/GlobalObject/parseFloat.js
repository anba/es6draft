/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {assertBuiltinFunction} = Assert;
const global = this;

/* 18.2.4  parseFloat (string) */

assertBuiltinFunction(global.parseFloat, "parseFloat", 1);

// no functional changes in comparison to ES5.1
