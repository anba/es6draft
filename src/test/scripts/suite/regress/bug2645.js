/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.14 Function Declaration Instantiation: Duplicate variable scoped bindings not checked
// https://bugs.ecmascript.org/show_bug.cgi?id=2645

// no crash
(function() {
  var a;
  var a;
})();
