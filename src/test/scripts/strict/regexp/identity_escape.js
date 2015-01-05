/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// UnicodeIDContinue must not appear in identity escape.
assertSyntaxError(`/\\a/`);
assertSyntaxError(`/\\A/`);
assertSyntaxError(`/\\_/`);
assertSyntaxError(`/\\\u00C0/`);
assertSyntaxError(`/\\\u0660/`);

// $, U+200C (ZERO-WIDTH NON-JOINER) and U+200D (ZERO-WIDTH JOINER) are IdentifierPart
// but not UnicodeIDContinue, so they are allowed to appear in identity escape sequences.
RegExp(`/\\$/`);
RegExp("\\\u200c");
RegExp("\\\u200d");
