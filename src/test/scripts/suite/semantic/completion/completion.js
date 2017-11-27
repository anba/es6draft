/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

const True = () => true;
const False = () => false;

assertSame(void 0, eval(`var c = 0; L: { do { if (c) break L; c = 1; } while (True()); }`));
assertSame(void 0, eval(`var c = 0; L: if (True()) do if (c) break L; else c = 1; while(True());`));

assertSame(void 0, eval(`var c = 0; L: { while (True()) { if (c) break L; c = 1; } }`));
assertSame(void 0, eval(`var c = 0; L: if (True()) while (True()) if (c) break L; else c = 1;`));

assertSame(void 0, eval(`var c = 0; L: { for (;;) { if (c) break L; c = 1; } }`));
assertSame(void 0, eval(`var c = 0; L: if (True()) for (;;) if (c) break L; else c = 1;`));

assertSame(1, eval(`L: { { 1; break L; } 2; }`));
assertSame(1, eval(`L: { while (True()) { 1; break L; } 2; }`));

assertSame(void 0, eval(`L: { 1; if (True()) break L; 2; 3; }`));
assertSame(3, eval(`L: { 1; if (False()) break L; 2; 3; }`));

assertSame(void 0, eval(`1; K: { L: { if (True()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { L: { if (False()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { L: { if (True()) break L; else break K; } }`));
assertSame(void 0, eval(`1; K: { L: { if (False()) break L; else break K; } }`));

assertSame(void 0, eval(`1; K: { 2; L: { if (True()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { 2; L: { if (False()) break L; break K; } }`));
assertSame(void 0, eval(`1; K: { 2; L: { if (True()) break L; else break K; } }`));
assertSame(void 0, eval(`1; K: { 2; L: { if (False()) break L; else break K; } }`));

assertSame(2, eval(`L: { if (True()) { 2; break L; } 1; }`));
assertSame(1, eval(`L: { if (False()) { 2; break L; } 1; }`));

assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) break L; c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) with ({}) break L; c = 1; } }`));

assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) while (True()) break L; c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) do break L; while (True()); c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) for (;True();) break L; c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) switch (0) { default: break L; } c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) try { break L; } catch (e) { } c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) try { throw null } catch (e) { break L; } c = 1; } }`));
assertSame(void 0, eval(`L: { var c = 0; while (True()) { if (c) try { } finally { break L; } c = 1; } }`));
assertSame(void 0, eval(`var c = 0; while (True()) { if (c) { switch (0) { } break; } c = 1; }`));
assertSame(void 0, eval(`var c = 0; while (True()) { if (c) { try { } finally { break; } } c = 1; }`));
assertSame(void 0, eval(`var c = 0; while (True()) { if (c) { try { -1; } finally { break; } -2; } c = 1; }`));
