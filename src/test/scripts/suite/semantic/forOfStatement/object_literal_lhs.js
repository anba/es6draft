/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

var value;
for ({set p(v) { value = v }}.p of ["ok"]) ;
assertSame("ok", value);
