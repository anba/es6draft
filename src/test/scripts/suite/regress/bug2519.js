/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 11.6.1.1 Identifier, Early Error: Allow Unicode escapes in "yield" if not used as a keyword
// https://bugs.ecmascript.org/show_bug.cgi?id=2519

function f1(){ var \u0079ield }
function f2(){ ! \u0079ield }

assertSyntaxError(`function* g(){ var \\u0079ield }`);
assertSyntaxError(`function* g(){ ! \\u0079ield }`);
