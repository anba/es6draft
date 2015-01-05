/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// 11.8.6 Template Literal: OctalEscapeSequence in template literals
// https://bugs.ecmascript.org/show_bug.cgi?id=2882

// Single NULL escape allowed
assertSame("\0", `\0`);
assertSame("\0" + "a", `\0a`);

// NULL escape followed DecimalDigit not allowed
assertSyntaxError("`\\00`");
assertSyntaxError("`\\01`");
assertSyntaxError("`\\02`");
assertSyntaxError("`\\03`");
assertSyntaxError("`\\04`");
assertSyntaxError("`\\05`");
assertSyntaxError("`\\06`");
assertSyntaxError("`\\07`");
assertSyntaxError("`\\08`");
assertSyntaxError("`\\09`");

// DecimalDigit escape not allowed
assertSyntaxError("`\\1`");
assertSyntaxError("`\\2`");
assertSyntaxError("`\\3`");
assertSyntaxError("`\\4`");
assertSyntaxError("`\\5`");
assertSyntaxError("`\\6`");
assertSyntaxError("`\\7`");
assertSyntaxError("`\\8`");
assertSyntaxError("`\\9`");
