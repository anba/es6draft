/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Null escape is allowed.
let nullEscape = /\0/;

// Decimal escape for captured groups is allowed.
let backReference = /(a)\1/;
let forwardReference = /\1(a)/;

// Decimal escape without matching capturing is invalid.
assertSyntaxError(`/\\1/`);
assertSyntaxError(`/\\2/`);
assertSyntaxError(`/\\3/`);
assertSyntaxError(`/\\4/`);
assertSyntaxError(`/\\5/`);
assertSyntaxError(`/\\6/`);
assertSyntaxError(`/\\7/`);
assertSyntaxError(`/\\8/`);
assertSyntaxError(`/\\9/`);

// Octal escape is invalid.
assertSyntaxError(`/\\00/`);
assertSyntaxError(`/\\01/`);
assertSyntaxError(`/\\02/`);
assertSyntaxError(`/\\03/`);
assertSyntaxError(`/\\04/`);
assertSyntaxError(`/\\05/`);
assertSyntaxError(`/\\06/`);
assertSyntaxError(`/\\07/`);
assertSyntaxError(`/\\08/`);
assertSyntaxError(`/\\09/`);
