/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Completion reform is incomplete
// https://bugs.ecmascript.org/show_bug.cgi?id=4540

const False = () => false;
const True = () => true;

assertSame(void 0, eval(`1; L: if (False()) { 2; break L; } else break L;`));
assertSame(2, eval(`1; L: if (True()) { 2; break L; } else break L;`));

assertSame(void 0, eval(`1; L: if (False()) { 2; break L; } else { break L; }`));
assertSame(2, eval(`1; L: if (True()) { 2; break L; } else { break L; }`));

assertSame(void 0, eval(`1; L: with ({}) break L;`));
assertSame(void 0, eval(`1; L: with ({}) { break L; }`));
