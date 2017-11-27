/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

const True = () => true;
const False = () => false;

// DoExpression in expression context
{
  assertSame(void 0, do { });
  assertSame(void 0, do { (do { }); });
  assertSame(0, do { 0; });
  assertSame(0, do { (do { 0; }); });

  assertSame(1, do { 1; L: { break L; } });
  assertSame(void 0, do { 1; do { break; } while (false); });
  assertSame(void 0, do { 1; do { continue; } while (false); });
  assertSame(void 0, do { 1; do { break; } while (False()); });
  assertSame(void 0, do { 1; do { continue; } while (False()); });

  assertSame(2, do { 1; L: { 2; break L; } });
  assertSame(2, do { 1; do { 2; break; } while (false); });
  assertSame(2, do { 1; do { 2; continue; } while (false); });
  assertSame(2, do { 1; do { 2; break; } while (False()); });
  assertSame(2, do { 1; do { 2; continue; } while (False()); });
}

// DoExpression in statement context
{
  assertSame(void 0, eval(`0; (do { });`));
  assertSame(1, eval(`0; (do { 1; });`));
  assertSame(2, eval(`0; (do { 1; L: { 2; break L; } });`));
  assertSame(void 0, eval(`0; L: { 1; (do { 2; break L; }); }`));

  assertSame(void 0, eval(`0; (do { 1; do { break; } while (false); });`));
  assertSame(void 0, eval(`0; (do { 1; do { continue; } while (false); });`));
  assertSame(void 0, eval(`0; (do { 1; do { break; } while (False()); });`));
  assertSame(void 0, eval(`0; (do { 1; do { continue; } while (False()); });`));

  assertSame(2, eval(`0; (do { 1; L: { 2; break L; } });`));
  assertSame(2, eval(`0; (do { 1; do { 2; break; } while (false); });`));
  assertSame(2, eval(`0; (do { 1; do { 2; continue; } while (false); });`));
  assertSame(2, eval(`0; (do { 1; do { 2; break; } while (False()); });`));
  assertSame(2, eval(`0; (do { 1; do { 2; continue; } while (False()); });`));
}

// DoExpression in function context
{
  assertSame(void 0, function(){ return do { }; }());
  assertSame(void 0, function(){ (do { return; }); }());
  assertSame(1, function(){ (do { return 1; }); }());
  assertSame(2, function(){ return (do { 2; }); }());
  assertSame(2, function(){ return (do { return 2; }); }());
  assertSame(2, function(){ try { return do { 1 }; } finally { (do { return 2; }); } }());
  assertSame(2, function(){ try { (do { return 1; }); } finally { (do { return 2; }); } }());
  assertSame(2, function(){ L: { (do { if (False()) break L; return 2; }); } return 3; }());
  assertSame(3, function(){ L: { (do { if (True()) break L; return 2; }); } return 3; }());
  assertSame(2, function(){ do { (do { if (False()) break; return 2; }); } while (False()); return 3; }());
  assertSame(3, function(){ do { (do { if (True()) break; return 2; }); } while (False()); return 3; }());
  assertSame(2, function(){ do { (do { if (False()) continue; return 2; }); } while (False()); return 3; }());
  assertSame(3, function(){ do { (do { if (True()) continue; return 2; }); } while (False()); return 3; }());

  assertSame(1, function(){ [do { if (True()) return 1; }] }());
  assertSame(void 0, function(){ L: [do { if (True()) break L; }] }());

  assertSame(1, function(){ L: try { break L; } finally { (do { return 1; }); } return 2; }());
  assertSame(1, function(){ L: try { (do { break L; }); } finally { return 1; } return 2; }());
  assertSame(1, function(){ L: try { (do { break L; }); } finally { (do { return 1; }); } return 2; }());
  assertSame(2, function(){ L: try { return 1; } finally { (do { break L; }); } return 2; }());
  assertSame(2, function(){ L: try { (do { return 1; }); } finally { break L; } return 2; }());
  assertSame(2, function(){ L: try { (do { return 1; }); } finally { (do { break L; }); } return 2; }());

  assertSame(1, function(){ L: try { if (True()) break L; } finally { (do { if (True()) return 1; }); } return 2; }());
  assertSame(1, function(){ L: try { (do { if (True()) break L; }); } finally { if (True()) return 1; } return 2; }());
  assertSame(1, function(){ L: try { (do { if (True()) break L; }); } finally { (do { if (True()) return 1; }); } return 2; }());
  assertSame(2, function(){ L: try { if (True()) return 1; } finally { (do { if (True()) break L; }); } return 2; }());
  assertSame(2, function(){ L: try { (do { if (True()) return 1; }); } finally { if (True()) break L; } return 2; }());
  assertSame(2, function(){ L: try { (do { if (True()) return 1; }); } finally { (do { if (True()) break L; }); } return 2; }());
}

// DoExpression in generator function context
{
  assertEquals([1], [...function*(){ (do { yield 1; }); }()]);
  assertEquals([1, 2, 3], [...function*(){ (do { yield 1; yield 2; }); yield 3; }()]);
}

// DoExpression in switch case expression
{
  assertSame(1, do { switch (0) { case do { 0; }: 1; } });
  assertSame(void 0, do { switch (0) { case do { break; }: 1; } });
}
