/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {assertBuiltinFunction} = Assert;
const global = this;

/* 18.2.2  isFinite (number) */

assertBuiltinFunction(global.isFinite, "isFinite", 1);

// no functional changes in comparison to ES5.1
