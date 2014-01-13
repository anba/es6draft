/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {assertBuiltinFunction} = Assert;
const global = this;

/* 18.3.3  encodeURI (uri) */

assertBuiltinFunction(global.encodeURI, "encodeURI", 1);

// no functional changes in comparison to ES5.1
