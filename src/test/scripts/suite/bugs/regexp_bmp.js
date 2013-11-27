/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

// If unicode-flag is not set, characters must be read as code-units instead of
// code-points. java.util.regex.Pattern does not support this mode, that means
// characters are always read as code-points. Until the underlying regular
// expression engine gets changed, assert the wrong behaviour in this test.

assertTrue(RegExp("^.$", "").test("\udbff\udfff"));
assertFalse(RegExp("^..$", "").test("\udbff\udfff"));
assertFalse(RegExp("^.\\udfff$", "").test("\udbff\udfff"));
assertFalse(RegExp("^\\udbff.$", "").test("\udbff\udfff"));

// assertFalse(RegExp("^\\udbff\\udfff$", "").test("\udbff\udfff"));
assertFalse(RegExp("^[\\udbff][\\udfff]$", "").test("\udbff\udfff"));
