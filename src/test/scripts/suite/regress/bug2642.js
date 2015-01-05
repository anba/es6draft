/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.14 Function Declaration Instantiation: Set "hasDuplicates" flag when "arguments" as parameter exists
// https://bugs.ecmascript.org/show_bug.cgi?id=2642

// no crash
(function(arguments){})();
