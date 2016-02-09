/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 9.4.4.6 CreateUnmappedArgumentsObject: Consider changing property creation order for "caller" and "callee"
// https://bugs.ecmascript.org/show_bug.cgi?id=4467

var keys = Object.getOwnPropertyNames(function(){ "use strict"; return arguments; }());
assertEquals(["length", "callee", "caller"], keys);
