/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.5.17 ClassDefinitionEvaluation: Necessary to preserve evaluation order for static/prototype method definitions?
// https://bugs.ecmascript.org/show_bug.cgi?id=2739

var log = "";
(class {
  static [(log += "|static", "")] () { }
  [(log += "|proto", "")] () { }
});
assertSame("|static|proto", log);

var log = "";
(class {
  [(log += "|proto", "")] () { }
  static [(log += "|static", "")] () { }
});
assertSame("|proto|static", log);
