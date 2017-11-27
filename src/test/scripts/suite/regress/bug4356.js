/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Iteration statements return undefined instead of empty
// https://bugs.ecmascript.org/show_bug.cgi?id=4356

assertSame(undefined, eval(`"ignored"; while (false) ;`));
assertSame(undefined, eval(`"ignored"; while (false) {}`));

assertSame(undefined, eval(`"ignored"; do ; while (false);`));
assertSame(undefined, eval(`"ignored"; do {} while (false);`));

assertSame(undefined, eval(`"ignored"; for (;false;) ;`));
assertSame(undefined, eval(`"ignored"; for (;false;) {}`));

assertSame(undefined, eval(`"ignored"; for (var k in null) ;`));
assertSame(undefined, eval(`"ignored"; for (var k in null) {}`));

assertSame(undefined, eval(`"ignored"; for (var k in []) ;`));
assertSame(undefined, eval(`"ignored"; for (var k in []) {}`));
