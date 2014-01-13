/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

assertTrue(RegExp("^.$", "u").test("\udbff\udfff"));
assertFalse(RegExp("^..$", "u").test("\udbff\udfff"));

assertFalse(RegExp("^.\\udfff$", "u").test("\udbff\udfff"));
assertFalse(RegExp("^\\udbff.$", "u").test("\udbff\udfff"));
assertTrue(RegExp("^\\udbff\\udfff$", "u").test("\udbff\udfff"));
assertFalse(RegExp("^[\\udbff][\\udfff]$", "u").test("\udbff\udfff"));
