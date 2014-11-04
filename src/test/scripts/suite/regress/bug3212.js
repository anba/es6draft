/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertSyntaxError
} = Assert;

// 11.8.4: Spec and implementations disagree about "\8" and "\9"
// https://bugs.ecmascript.org/show_bug.cgi?id=3212

assertSame("\u0007", "\7");
assertSame("8", "\8");
assertSame("9", "\9");

assertSyntaxError('"use strict"; "\\7"');
assertSame("8", eval('"use strict"; "\\8"'));
assertSame("9", eval('"use strict"; "\\9"'));

assertSyntaxError('`\\7`');
assertSyntaxError('`\\8`');
assertSyntaxError('`\\9`');

assertSyntaxError('"use strict"; `\\7`');
assertSyntaxError('"use strict"; `\\8`');
assertSyntaxError('"use strict"; `\\9`');
