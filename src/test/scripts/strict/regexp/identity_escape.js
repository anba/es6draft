/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// IdentifierPart (UnicodeIDContinue, $, _) must not appear in identity escape.
assertSyntaxError(`/\\$/`);
assertSyntaxError(`/\\a/`);
assertSyntaxError(`/\\A/`);
assertSyntaxError(`/\\_/`);
assertSyntaxError(`/\\\u00C0/`);
assertSyntaxError(`/\\\u0660/`);

// U+200C (ZERO-WIDTH NON-JOINER) and U+200D (ZERO-WIDTH JOINER) are IdentifierPart,
// but are explicitly allowed in the regular expression grammar to appear in identity
// escape sequences.
RegExp("\\\u200c");
RegExp("\\\u200d");
