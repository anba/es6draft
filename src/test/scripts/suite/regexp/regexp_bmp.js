/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

// If unicode-flag is not set, characters must be read as code-units instead of
// code-points. java.util.regex.Pattern does not support this mode, that means
// characters are always read as code-points. org.joni.Regex supports this mode,
// if properly configured.

assertFalse(RegExp("^.$", "").test("\udbff\udfff"));
assertTrue(RegExp("^..$", "").test("\udbff\udfff"));
assertTrue(RegExp("^.\\udfff$", "").test("\udbff\udfff"));
assertTrue(RegExp("^\\udbff.$", "").test("\udbff\udfff"));

assertTrue(RegExp("^\\udbff\\udfff$", "").test("\udbff\udfff"));
assertTrue(RegExp("^[\\udbff][\\udfff]$", "").test("\udbff\udfff"));
