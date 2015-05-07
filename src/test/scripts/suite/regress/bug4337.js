/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Annex E: Completion reform changes
// https://bugs.ecmascript.org/show_bug.cgi?id=4337

// If-Statement completion value
assertSame(undefined, eval(`0; if (false) ;`));
assertSame(undefined, eval(`0; if (true) ;`));
assertSame(undefined, eval(`0; if (false) ; else ;`));
assertSame(undefined, eval(`0; if (true) ; else ;`));

// Switch-Statement completion value
assertSame(1, eval(`switch (0) { case 0: 1; case 1: }`));
assertSame(1, eval(`switch (0) { case 0: 1; default: }`));

// Try-Statement completion value
assertSame(1, eval(`L: try { throw null } catch (e) { ; } finally { 1; break L; }`));
