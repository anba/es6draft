/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 13.6: ambiguity re do-while statement
// https://bugs.ecmascript.org/show_bug.cgi?id=4099

function noSemi() {
  do {} while (0)
}

function withSemi() {
  do {} while (0) ;
}
