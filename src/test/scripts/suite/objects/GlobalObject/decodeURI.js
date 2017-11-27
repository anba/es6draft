/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {assertBuiltinFunction} = Assert;
const global = this;

/* 18.3.1  decodeURI (encodedURI) */

assertBuiltinFunction(global.decodeURI, "decodeURI", 1);

// no functional changes in comparison to ES5.1
