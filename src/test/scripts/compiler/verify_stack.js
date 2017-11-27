/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
// No Crash
function f1() { (0 > [for (x of []) 1]); }
function f2() { (0 > [for (x of []) ref]) }
function f3() { f(1,2,ref*3) }
function f4() { ref * gen(); };
function f5() { try { return } finally {} }
function f6() { try {} finally { throw 0 } }
function f7() { do { return } while (0) }
function f8() { while (1) ; }
function f9() { for (;;) ; }
function f10() { for (;;) break; }
function f11() { for (k in o) ; }
function f12() { switch(v) { case d: return; default : return } }
function f13() { L: return }
function f14() { try { return } catch(e) { return } }
function f15() { if (x) { return 0 } else { return 1 } }
function f16() { for (let i = 0;;) continue; }

function* g1() { (0 > [for (x of []) 1]); }
function* g2() { (0 > [for (x of []) yield 0]) }
function* g3() { f(1,2,yield*3) }
function* g4() { yield* gen(); };
function* g5() { try { return } finally {} }
function* g6() { try {} finally { throw 0 } }
function* g7() { do { return } while (0) }
function* g8() { while (1) ; }
function* g9() { for (;;) ; }
function* g10() { for (;;) break; }
function* g11() { for (k in o) ; }
function* g12() { switch(v) { case d: return; default : return } }
function* g13() { L: return }
function* g14() { try { return } catch(e) { return } }
function* g15() { if (x) { return 0 } else { return 1 } }
function* g16() { for (let i = 0;;) continue; }
function* g17() { f(1,2,yield) }
function* g18() { f(1,2,yield*3) }
function* g19() { 2.4 - (yield) }
function* g19() { 2.4 - (yield*3) }

[...(for ({a = eval("let letInGenCompr = 0;")} of [{}]) 123)];
