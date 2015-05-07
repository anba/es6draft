/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

const True = () => true;
const False = () => false;

assertSame(1, eval(`var c = 0; L: { do { if (c) break L; c = 1; } while (True()); }`));
assertSame(1, eval(`var c = 0; L: if (True()) do if (c) break L; else c = 1; while(True());`));

assertSame(1, eval(`var c = 0; L: { while (True()) { if (c) break L; c = 1; } }`));
assertSame(1, eval(`var c = 0; L: if (True()) while (True()) if (c) break L; else c = 1;`));

assertSame(1, eval(`var c = 0; L: { for (;;) { if (c) break L; c = 1; } }`));
assertSame(1, eval(`var c = 0; L: if (True()) for (;;) if (c) break L; else c = 1;`));

assertSame(1, eval(`L: { { 1; break L; } 2; }`));
assertSame(1, eval(`L: { while (True()) { 1; break L; } 2; }`));

assertSame(1, eval(`L: { 1; if (True()) break L; 2; 3; }`));
assertSame(3, eval(`L: { 1; if (False()) break L; 2; 3; }`));

assertSame(void 0, eval(`1; K: { L: { if (True()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { L: { if (False()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { L: { if (True()) break L; else break K; } }`));
assertSame(void 0, eval(`1; K: { L: { if (False()) break L; else break K; } }`));

assertSame(void 0, eval(`1; K: { 2; L: { if (True()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { 2; L: { if (False()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { 2; L: { if (True()) break L; else break K; } }`));
assertSame(2, eval(`1; K: { 2; L: { if (False()) break L; else break K; } }`));
